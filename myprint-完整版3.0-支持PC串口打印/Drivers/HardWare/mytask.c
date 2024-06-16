#include "mytask.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"
#include "printer.h"
#include "event_groups.h"
#include "BLE.h"
#include "serial.h"
#include "LED.h"
#include "em_adc.h"
#include "motor.h"
#include "stdio.h"
#include "KEY.h"
#include "stdlib.h"

struct Dev_state
{
    uint8_t power;
    float   temp;
    uint8_t paper_state;
    uint8_t printer_state;
    uint8_t error_state;//0:设备正常   1:缺纸异常  2:高温异常
};

struct Dev_state dev={0,0,0,0,0,};


static QueueHandle_t  print_data_queue;
static SemaphoreHandle_t g_printer_mutex;
static EventGroupHandle_t   g_xEventPrintStart;  //用于通知打印开始
/*task init*/
void task_init(void)
{
    /*初始一个队列*/
    print_data_queue = xQueueCreate(150,ONE_LINE_LEN); //最大100个数据，即100行数据
    /*创建事件组*/
    g_xEventPrintStart = xEventGroupCreate();
    /*创建互斥量*/
    g_printer_mutex = xSemaphoreCreateMutex();
}
//写打印数据到消息队列
void write_to_print_data_queue(uint8_t* cmd_buffer)  
{
    BaseType_t xHigherPriorityTaskWoken;
    xQueueSendToBackFromISR(print_data_queue,cmd_buffer,&xHigherPriorityTaskWoken);

    if( xHigherPriorityTaskWoken == pdTRUE)
    {   
        // 如果有更高优先级的任务需要唤醒，则进行任务切换
        portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
    }
}


void Start_pinter_task(void)
{
    BaseType_t xHigherPriorityTaskWoken;
    xEventGroupSetBitsFromISR(g_xEventPrintStart,0x01,&xHigherPriorityTaskWoken);
    if(xHigherPriorityTaskWoken == pdTRUE)
    {
        // 如果有更高优先级的任务需要唤醒，则进行任务切换
        portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
    }
}

uint8_t error_check(void)
{
    get_power_temp(&dev.power,&dev.temp); //检测电量与温度
    dev.paper_state = Paper_Detection(); //检测是否缺纸
    if(dev.paper_state == PAPER_STATUS_LACK)
    {
        dev.error_state = 1;
    }else if (dev.temp > 60)
    {
        dev.error_state = 2;
    }else
    {
        dev.error_state = 0;
    }

    return dev.error_state;
}

//打印任务
void task_printer(void *Parame)
{
    uint8_t print_data[ONE_LINE_LEN] = {0};

    while (1)
    {  
        /*等待事件成立，即等待执行开始打印任务的信号*/
        xEventGroupWaitBits(g_xEventPrintStart,0x01,pdTRUE, pdTRUE,portMAX_DELAY);
        xSemaphoreTake(g_printer_mutex,portMAX_DELAY);
        //开始打印
        printf("print start\n");
        uint8_t cnt = 0;
        dev.printer_state = 1;
		printer_start();
       while (1)
        {
            //读队列
            xQueueReceive(print_data_queue,print_data,portMAX_DELAY);
            
            uint8_t error = error_check();
            if (error == 0)
            {
                printer_printing_one_line_data(print_data);

                if(uxQueueMessagesWaiting(print_data_queue) == 0) 
                {
                    //队列可读数据为空 则说明打印结束
                    clean_blepack_count();
                    clean_serialpack_count();
                    printer_stop();
                    motor_run_step(140);
                    motor_stop();
                    printf("打印结束!\n");
                    break;
                }
            }else
            {  
                //打印机异常
                printer_stop();
                motor_stop();
                if (error == 1 && cnt == 0)
                {
                    printf("打印停止:缺纸!\n");
                    cnt = 1;
                }
                if (error == 2 && cnt == 0)
                {
                    printf("打印停止:温度过高!\n");
                    cnt = 1;
                }
                if(uxQueueMessagesWaiting(print_data_queue) == 0) 
                {
                    //队列可读数据为空 即便错误也需要把队列读完在退出,主要是为了清空队列
                    printf("队列清空!\n");
                    clean_blepack_count();
                    break;
                }
            }
            
            vTaskDelay(10);
        }
        dev.printer_state = 0;
        xSemaphoreGive(g_printer_mutex);
        vTaskDelay(100);


    }
    
}

/*打印机测试程序：无异常检测机制*/
void printer_test(void)
{
    xSemaphoreTake(g_printer_mutex,portMAX_DELAY);
    printf("开始测试打印\n");
    dev.printer_state = 1;
    printer_stb_test();

    motor_run_step(100);
    motor_stop();
    dev.printer_state = 0;
    xSemaphoreGive(g_printer_mutex);
    printf("打印测试结束\n");
}


//设备状态检测
void task_dev_check(void *Parame)
{   uint8_t cnt = 0;
    while (1)
    {
        error_check(); //检测是否有错误

        uint8_t state[4];
        state[0] = dev.power;
        state[1] = dev.temp;
        state[2] = dev.paper_state;
        state[3] = dev.printer_state;
        cnt ++;
        //向串口发送数据
        if(cnt > 50)
        { 
            printf("power:%d\n",state[0]);
            printf("temp:%d\n",state[1]);
            printf("paper_state:%d\n",state[2]);
            printf("pinter_state:%d\n",state[3]);
        
            if(dev.error_state == 1)
            {
                printf("缺纸!\n");
            }
            if (dev.error_state == 2)
            {
                printf("温度过高!\n");
            }
            cnt = 0;
        }
        //通过蓝牙发送数据，,打印开始禁止通过上报数据 
        if(dev.printer_state == 0)
        { 
            ble_report(state,4);
            serial_report(state,4);
        }
        ble_status_data_clean(); // 该函数必须周期性调用，目的过滤连接状态，清空缓存区
        vTaskDelay(100);
    }
}

void task_led(void *Parame)
{
    uint8_t state = 0;
    while (1)
    {
       state = 0;
       if(dev.printer_state == 1)
       {
            state = 1; // 正在打印
       }
       if(dev.error_state > 0)
       {
            state = 2; //设备异常
       } 

       switch (state)
       {
        case 0:
                LED_Set_status(LED_ON);
            break;
        case 1:
                LED_Set_status(LED_LOW_FLASH);
            break;
        case 2:
                LED_Set_status(LED_QUICK_FLASH);
            break;
        default:
            break;
       }
    vTaskDelay(10);
    }
    
}


//按键任务
void task_key(void *Parame)
{
    while (1)
    {
        KEY_Value value = KEY_Scan();

        switch (value)
        {
        case KEY_Press:
            /*短按:打印测试*/
            printer_test();
            break;
        case KEY_LongPrees:
            /*长按:电机走一步*/
            motor_run_step(1);
            break;
        case KEY_LongPreesLose:
            /*长按松:电机停止*/
            motor_stop();
            break;
        default:
            break;
        }

        vTaskDelay(20);
    }
    
}

void esp_print_tasks(void)
{
    char *pbuffer = (char *)calloc(1,500);
    printf("--------------- heap:---------------------\r\n");
    vTaskList(pbuffer);
    printf("%s", pbuffer);
    printf("min:%d\n",xPortGetMinimumEverFreeHeapSize());
    printf("----------------------------------------------\r\n");
    free(pbuffer);
}

void task_system(void *Parame)
{
    while(1) {
        esp_print_tasks();
        vTaskDelay(3000);
    }
}
