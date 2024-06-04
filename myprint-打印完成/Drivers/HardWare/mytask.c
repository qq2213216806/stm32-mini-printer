#include "mytask.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "printer.h"
#include "event_groups.h"
#include "BLE.h"
#include "motor.h"
#include "stdio.h"

static QueueHandle_t  print_data_queue;
static EventGroupHandle_t   g_xEventPrintStart;  //用于通知打印开始
/*task init*/
void task_init(void)
{
    /*初始一个队列*/
    print_data_queue = xQueueCreate(100,ONE_LINE_LEN); //最大200个数据，即200行数据
    /*创建事件组*/
    g_xEventPrintStart = xEventGroupCreate();
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
//打印任务
void task_printer(void *Parame)
{
    uint8_t print_data[ONE_LINE_LEN] = {0};

    while (1)
    {  
        /*等待事件成立，即等待执行开始打印任务的信号*/
        xEventGroupWaitBits(g_xEventPrintStart,0x01,pdFALSE, pdTRUE,portMAX_DELAY);
        //开始打印
        printf("print start\n");
		printer_start();
       while (1)
        {
			printf("data:\n");
            //读队列
            xQueueReceive(print_data_queue,print_data,portMAX_DELAY);
			
			printer_printing_one_line_data(print_data);
				
            
            if(uxQueueMessagesWaiting(print_data_queue) == 0) 
            {
                //队列可读数据为空 则说明打印结束
                clean_blepack_count();
                printer_stop();
				motor_run_step(140);
				motor_stop();
                break;
            }
        }
        vTaskDelay(500);
    }
    
}
