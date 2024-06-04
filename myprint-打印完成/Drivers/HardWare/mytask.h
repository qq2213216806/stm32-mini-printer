#ifndef __MYTASK_H
#define __MYTASK_H
#include "stm32f1xx_hal.h"
void task_init(void);
void Start_pinter_task(void);
void write_to_print_data_queue(uint8_t* cmd_buffer);
void task_printer(void *Parame);
#endif /*__TASK_H*/
