#ifndef __LED_H
#define __LED_H
#include "stm32f1xx_hal.h"
typedef enum
{
    LED_OFF = 0,
    LED_ON,
    LED_LOW_FLASH,
    LED_QUICK_FLASH,
} LED_STATUS;

void LED_Set_status(LED_STATUS status);
uint8_t Paper_Detection(void);
#endif /*__LED_H*/
