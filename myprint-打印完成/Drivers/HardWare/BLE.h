#ifndef __BLE_H
#define __BLE_H

#include "stm32f1xx_hal.h"
/**
 * @brief 初始化BLE
 * 
 */
void init_ble(void);

/**
 * @brief 清空接收包统计
 * 
 */
void clean_blepack_count(void);

/**
 * 获取接收行数
*/
uint32_t get_blepack_count(void);

void ble_status_data_clean(void);

void uart_cmd_handle(uint8_t data);

/**
 * @brief Get the ble connect object
 * 
 * @return true 
 * @return false 
 */
uint8_t get_ble_connect(void);

/**
 * @brief 
 * 
 */
void ble_report(void);

#endif /*__BLE_H*/
