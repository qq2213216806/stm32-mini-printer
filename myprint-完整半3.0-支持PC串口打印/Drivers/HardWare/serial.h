#ifndef __SERIAL_H
#define __SERIAL_H
#include "stm32f1xx_hal.h"

void serial_cmd_handle(uint8_t data);
void clean_serialpack_count(void);

#endif /*__SERIAL_H*/
