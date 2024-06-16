#ifndef __MOTOR_H
#define __MOTOR_H
#include "stm32f1xx_hal.h"

#define PIN_MOTOR_AP 23
#define PIN_MOTOR_AM 22
#define PIN_MOTOR_BP 21
#define PIN_MOTOR_BM 19


void motor_init(void);
void motor_run_step(uint8_t step);
void motor_stop(void);
#endif /*__MOTOR_H*/
