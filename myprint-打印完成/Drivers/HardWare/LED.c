#include "LED.h"
#include "gpio.h"
#include "FreeRTOS.h"
#include "task.h"

void LED_flash(int ms)
{
    HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_RESET);
    vTaskDelay(ms);
    HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_SET);
    vTaskDelay(ms);
    HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_RESET);
    vTaskDelay(ms);
    HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_SET);
    vTaskDelay(ms);
}

void LED_Set_status(LED_STATUS status)
{
    switch (status)
    {
    case LED_OFF:
        HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_SET);
        break;
    case LED_ON:
        HAL_GPIO_WritePin(LED_GPIO_Port,LED_Pin,GPIO_PIN_RESET);
        break;
    case LED_LOW_FLASH:
        LED_flash(500);
        break;
    case LED_QUICK_FLASH:
        LED_flash(100);
        break;
    default:
        break;
    }
}

/*纸张检测  0:不缺纸 1:缺纸*/

uint8_t Paper_Detection(void)
{
    return HAL_GPIO_ReadPin(PHINT_GPIO_Port,PHINT_Pin);
}
