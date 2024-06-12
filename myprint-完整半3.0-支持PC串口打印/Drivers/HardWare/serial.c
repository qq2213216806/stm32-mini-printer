#include "serial.h"
//#include "usart.h"
#include "FreeRTOS.h"
#include "task.h"
//#include "stdio.h"
#include "string.h"
#include "mytask.h"

int serial_cmd_index = 0;
uint8_t serial_cmd_buffer[100];
uint8_t serial_Start_flag = 0; //用于指示打印是否已经开始,屏蔽重复开始信号  1：已经开始 0：没有开始
uint32_t serial_packcount = 0;
//透传数据处理
void serial_cmd_handle(uint8_t data)
{
    serial_cmd_buffer[serial_cmd_index++] = data;
    //开始打印指令
    if (serial_cmd_buffer[0] == 0xA6 && serial_cmd_buffer[1] == 0xA6 && serial_cmd_buffer[2] == 0xA6 && serial_cmd_buffer[3] == 0xA6)
    {
        //清空缓存区
        serial_cmd_index = 0;
        memset(serial_cmd_buffer, 0, sizeof(serial_cmd_buffer));
        return;
    }
    //打印图像信息，存储到消息队列中
    if (serial_cmd_index >= 48)
    {
      
      //write_to_printbuffer(cmd_buffer, cmd_index);  //写打印数据到消息队列
     write_to_print_data_queue(serial_cmd_buffer);
      serial_packcount++;
      if(serial_packcount > 50 && serial_Start_flag == 0)
      {
        //printf("read finish\n");
		    serial_Start_flag = 1;
        Start_pinter_task();
      }
      serial_cmd_index = 0;
      memset(serial_cmd_buffer, 0, sizeof(serial_cmd_buffer));
      // printf("packcount = %d\n",packcount);
    }
     //结束打印指令
    if (serial_cmd_buffer[0] == 0xA7 && serial_cmd_buffer[1] == 0xA7 && serial_cmd_buffer[2] == 0xA7 && serial_cmd_buffer[3] == 0xA7)
    {
        //清空缓存区
        serial_cmd_index = 0;
        memset(serial_cmd_buffer, 0, sizeof(serial_cmd_buffer));
        return;
    }
    if (serial_cmd_index >= 100)
    {
       serial_cmd_index = 0;
        memset(serial_cmd_buffer, 0, sizeof(serial_cmd_buffer));
    }
    
}
void clean_serialpack_count()
{
  serial_Start_flag = 0;
  serial_packcount = 0;
}
