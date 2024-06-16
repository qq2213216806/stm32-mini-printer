#include "BLE.h"
#include "usart.h"
#include "FreeRTOS.h"
#include "task.h"
#include "stdio.h"
#include "string.h"
#include "mytask.h"

uint8_t bleConnected = 1;
uint32_t packcount = 0;
extern UART_HandleTypeDef huart2;
extern UART_HandleTypeDef huart1;
// 定义一个字符串
char *ble_in_at_mode = "+++";
char *ble_set_status = "AT+STATUS=0\r\n";        // 关闭设备状态显示
char *ble_query_status = "AT+STATUS?\r\n";       // 关闭设备状态显示
char *ble_query_name = "AT+NAME?\r\n";           // AT+NAME=RF-CRAZY\r\nOK
char *ble_set_name = "AT+NAME=Mini-Printer\r\n"; // OK 大写？
// char *ble_set_name = "AT+NAME=RF-CRAZY\r\n"; //OK 大写？
char *ble_out_at_mode = "AT+EXIT\r\n";  //退出AT模式

typedef enum
{
  BLE_INIT_START = 0,
  BLE_IN_AT_MODE,
  BLE_IN_AT_MODE_SUCCESS,
  BLE_CLOSE_STATUS,
  BLE_CLOSE_STATUS_SUCCESS,
  BLE_QUERY_STATUS,
  BLE_QUERY_STATUS0_SUCCESS,
  BLE_QUERY_NAME,
  BLE_NEED_SET_NAME,
  BLE_NONEED_SET_NAME,
  BLE_SET_NAME,
  BLE_SET_NAME_SUCCESS,
  BLE_OUT_AT_MODE,
  BLE_INIT_FINISH,
  BLE_RESET,
} e_ble_init_step;

e_ble_init_step g_ble_init_step = BLE_INIT_START;
uint8_t need_reboot_ble = 0;
uint8_t Start_flag = 0; //用于指示打印是否已经开始,屏蔽重复开始信号  1：已经开始 0：没有开始
void clean_blepack_count()
{
  Start_flag = 0;
  packcount = 0;
}

uint32_t get_blepack_count()
{
  return packcount;
}

uint8_t get_ble_connect()
{
  return bleConnected;
}
//透传上报状态信息
void ble_report(uint8_t *data,uint8_t len)
{
  /*
  if (get_ble_connect())
  {
    device_state_t *pdevice = get_device_state();
    uint8_t status[4];
    status[0] = pdevice->battery;
    status[1] = pdevice->temperature;
    status[2] = pdevice->paper_state;
    status[3] = pdevice->printer_state;
    HAL_UART_Transmit(&huart2, (uint8_t *)&status, sizeof(status), 0xffff);
  }
  */
  HAL_UART_Transmit(&huart2,data,len,HAL_MAX_DELAY);
}

int cmd_index = 0;
uint8_t cmd_buffer[100];
uint8_t need_clean_ble_status = 0;
//uint32_t lineCount = 0;

//透传数据处理
void ble_cmd_handle(uint8_t data)
{
  cmd_buffer[cmd_index++] = data;
  char *ptr_char = (char *)cmd_buffer;
  if (g_ble_init_step == BLE_INIT_FINISH)
  { 
    //过滤模组状态数据
    if (strstr(ptr_char, "CONNECTED") != NULL)
    {
      need_clean_ble_status = 1;
    }
    if (strstr(ptr_char, "DISCONNECTED") != NULL)
    {
      need_clean_ble_status = 1;
    }
    if (strstr(ptr_char, "DEVICE ERROR") != NULL)
    {
      need_clean_ble_status = 1;
    }
    //打印设置指令处理
    if (cmd_index == 5)
    {
      //打印密度设置
      if (cmd_buffer[0] == 0xA5 && cmd_buffer[1] == 0xA5 && cmd_buffer[2] == 0xA5 && cmd_buffer[3] == 0xA5)
      {
        if (cmd_buffer[4] == 1)
        {
          //set_heat_density(30);
        }
        else if (cmd_buffer[4] == 2)
        {
          //set_heat_density(60);
        }
        else
        {
          //set_heat_density(100);
        }
        cmd_index = 0;
        memset(cmd_buffer, 0, sizeof(cmd_buffer));
        return;
      }
      //开始打印指令
      if (cmd_buffer[0] == 0xA6 && cmd_buffer[1] == 0xA6 && cmd_buffer[2] == 0xA6 && cmd_buffer[3] == 0xA6)
      {
        //set_read_ble_finish(1);
        /*
        if (Start_flag == 0)
        {
          //printf("read finish\n");
		      Start_flag = 1;
          Start_pinter_task();
        }
        */
        cmd_index = 0;
        memset(cmd_buffer, 0, sizeof(cmd_buffer));
        return;
      }
    }
    //打印图像信息，存储到消息队列中
    if (cmd_index >= 48)
    {
      
      //write_to_printbuffer(cmd_buffer, cmd_index);  //写打印数据到消息队列
     write_to_print_data_queue(cmd_buffer);
      packcount++;
      if(packcount > 50 && Start_flag == 0)
      {
        //printf("read finish\n");
		    Start_flag = 1;
        Start_pinter_task();
      }
      cmd_index = 0;
      memset(cmd_buffer, 0, sizeof(cmd_buffer));
      // printf("packcount = %d\n",packcount);
    }
  }
  else
  {
    //初始化过程处理
    if (strstr(ptr_char, "OK\r\n") != NULL)
    {
      if (g_ble_init_step == BLE_IN_AT_MODE)
      {
        g_ble_init_step = BLE_IN_AT_MODE_SUCCESS;
      }
      else if (g_ble_init_step == BLE_CLOSE_STATUS)
      {
        g_ble_init_step = BLE_CLOSE_STATUS_SUCCESS;
      }
      else if (g_ble_init_step == BLE_QUERY_NAME)
      {
        if (strstr(ptr_char, "RF-CRAZY") != NULL){
          g_ble_init_step = BLE_NEED_SET_NAME;
        }
        else{
          g_ble_init_step = BLE_NONEED_SET_NAME;
        }
      }
      else if (g_ble_init_step == BLE_SET_NAME)
      {
        g_ble_init_step = BLE_SET_NAME_SUCCESS;
      }
      else if (g_ble_init_step == BLE_OUT_AT_MODE)
      {
        g_ble_init_step = BLE_INIT_FINISH;
      }
      else if (g_ble_init_step == BLE_RESET)
      {
        g_ble_init_step = BLE_INIT_START;
      }
      else if (g_ble_init_step == BLE_QUERY_STATUS)
      {
        if (strstr(ptr_char, "AT+STATUS=0") != NULL)
        {
          g_ble_init_step = BLE_QUERY_STATUS0_SUCCESS;
        }
        else
        {
          g_ble_init_step = BLE_CLOSE_STATUS;
        }
      }
      cmd_index = 0;
      memset(cmd_buffer, 0, sizeof(cmd_buffer));
      return;
    }
    if (strstr(ptr_char, "ERROR\r\n") != NULL)
    {
      g_ble_init_step = BLE_RESET;
    }
    if (cmd_index >= sizeof(cmd_buffer))
    {
      cmd_index = 0;
    }
  }
}

int retry_count = 0;

void init_ble()
{
  while (1)
  {
    retry_count++;
    HAL_Delay(200);
    if (g_ble_init_step == BLE_INIT_START || g_ble_init_step == BLE_IN_AT_MODE)
    {
      printf("BLE:正进入AT模式\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_in_at_mode, strlen(ble_in_at_mode), 0xffff);
      g_ble_init_step = BLE_IN_AT_MODE;
    }
    else if (g_ble_init_step == BLE_IN_AT_MODE_SUCCESS || g_ble_init_step == BLE_CLOSE_STATUS)
    {
      printf("BLE:正在设置status 关闭状态显示\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_set_status, strlen(ble_set_status), 0xffff);
      g_ble_init_step = BLE_CLOSE_STATUS;
    }
    else if (g_ble_init_step == BLE_CLOSE_STATUS_SUCCESS || g_ble_init_step == BLE_QUERY_STATUS)
    {
      printf("BLE:正查询状态是否为0\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_query_status, strlen(ble_query_status), 0xffff);
      g_ble_init_step = BLE_QUERY_STATUS;
    }
    else if (g_ble_init_step == BLE_QUERY_STATUS0_SUCCESS || g_ble_init_step == BLE_QUERY_NAME)
    {
      printf("BLE:正查询设备名称\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_query_name, strlen(ble_query_name), 0xffff);
      g_ble_init_step = BLE_QUERY_NAME;
    }
    else if (g_ble_init_step == BLE_NEED_SET_NAME || g_ble_init_step == BLE_SET_NAME)
    {
      printf("BLE:正设置设备名称\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_set_name, strlen(ble_set_name), 0xffff);
      g_ble_init_step = BLE_SET_NAME;
      need_reboot_ble = 1;
    }
    else if (g_ble_init_step == BLE_SET_NAME_SUCCESS || g_ble_init_step == BLE_NONEED_SET_NAME || g_ble_init_step == BLE_OUT_AT_MODE)
    {
      printf("BLE:正退出AT模式\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_out_at_mode, strlen(ble_out_at_mode), 0xffff);
      g_ble_init_step = BLE_OUT_AT_MODE;
    }
    else if (g_ble_init_step == BLE_INIT_FINISH)
    {
      break;
    }
    else if (g_ble_init_step == BLE_RESET)
    {
      printf("BLE:BLE RESET 退出AT模式\n");
      HAL_UART_Transmit(&huart2, (uint8_t *)ble_out_at_mode, strlen(ble_out_at_mode), 0xffff);
    }
    printf("g_ble_init_step = %d\n", g_ble_init_step);
  }
  if (need_reboot_ble)
  {
    printf("配置完成-请重启设备使用\n");
  }
  else
  {
    printf("配置完成-可以正常使用\n");
  }
  HAL_Delay(1000);
  cmd_index = 0;
  memset(cmd_buffer, 0, sizeof(cmd_buffer));
}

// 这步操作是因为厂家的蓝牙模组，现在status只关了busy、connect timeout、device start、wake up
// 所以需要把CONNECTED DISCONNECTED DEVICE ERROR这些业务无关数据清掉
//// 该函数必须周期性调用，目的过滤连接状态，清空缓存区,避免干扰图像数据
void ble_status_data_clean()
{
  if (need_clean_ble_status)
  {
    vTaskDelay(200);
    printf("clean --->%s\n", cmd_buffer);
    cmd_index = 0;
    memset(cmd_buffer, 0, sizeof(cmd_buffer));
    need_clean_ble_status = 0;
  }
}
