#ifndef __PRINTER_H
#define __PRINTER_H
#include "stm32f1xx_hal.h"

#define PIN_STB1 26
#define PIN_STB2 27
#define PIN_STB3 14
#define PIN_STB4 32
#define PIN_STB5 33
#define PIN_STB6 25
#define PIN_VHEN 17
#define PIN_LAT 12

#define LOW               0x0
#define HIGH              0x1

#define PRINT_TIME 2500 // 加热时间
#define PRINT_END_TIME  200 //冷却时间
#define LAT_TIME	1 //锁存时间

#define ONE_LINE_LEN 48 //每行字节数


void printer_init(void);
void printer_start(void);
void printer_stop(void);
void printer_printing_one_line_data(uint8_t *data);
void printer_start_printing(uint8_t *data,int len);
void printer_stb_test(void);
#endif /*__PRINTER_H*/
