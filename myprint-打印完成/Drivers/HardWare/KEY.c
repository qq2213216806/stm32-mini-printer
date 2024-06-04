#include "KEY.h"
#include "gpio.h"
#include "FreeRTOS.h"
#include "task.h"
#include "stdio.h"
#define SHORT_PRESS_TIME 1000


/*按键电平
    0:按下
    1：松开
*/
int KEY_Level(void)
{
    if(HAL_GPIO_ReadPin(BTN_GPIO_Port,BTN_Pin) == GPIO_PIN_RESET )
    {
        vTaskDelay(10);
        if( HAL_GPIO_ReadPin(BTN_GPIO_Port,BTN_Pin) == GPIO_PIN_RESET)
        {
            return 0;
        }
    }
    return 1;
}   


uint8_t keyIsPress = 0;
uint32_t clicktime = 0;

KEY_Value KEY_Scan(void)
{
    if(keyIsPress == 0)
    {
        if(HAL_GPIO_ReadPin(BTN_GPIO_Port,BTN_Pin) == GPIO_PIN_RESET )
        {
            HAL_Delay(10);
            if( HAL_GPIO_ReadPin(BTN_GPIO_Port,BTN_Pin) == GPIO_PIN_RESET)
            {
                keyIsPress = 1; //按键按下
                clicktime = HAL_GetTick(); //获取当前系统时钟
            }
        }
    }

    if(keyIsPress == 1)
    {
        if(HAL_GPIO_ReadPin(BTN_GPIO_Port,BTN_Pin) == GPIO_PIN_SET)   /*按键松开*/
        {
            if(HAL_GetTick() - clicktime > SHORT_PRESS_TIME) /*判定为长按松开*/
            {
                //printf("长按松\n");
                keyIsPress = 0;
                return KEY_LongPreesLose;
            }else /*判定为短按*/
            {   
                //printf("短按\n");
                keyIsPress = 0;
                return KEY_Press;
            }
        }else
        {
            if(HAL_GetTick() - clicktime > SHORT_PRESS_TIME) /*判定为长按*/
            {
               // printf("长按\n");
                return KEY_LongPrees;
            }
        }

    }
    return KEY_Lose;
}
 
