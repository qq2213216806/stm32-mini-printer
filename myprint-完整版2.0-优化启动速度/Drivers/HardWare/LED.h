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

typedef enum{
    PAPER_STATUS_NORMAL = 0,
    PAPER_STATUS_LACK,
}paper_state;

void LED_Init(void);
void LED_Set_status(LED_STATUS status);
paper_state Paper_Detection(void);
#endif /*__LED_H*/
