#include "POWER.h"
#include "stm32f1xx_hal.h"
#include "adc.h"
#include "stdio.h"

uint16_t POWER_GET_ADC(void)
{
    HAL_ADC_Start(&hadc1);
    HAL_ADC_PollForConversion(&hadc1,100);  //等待转换完成
    if(HAL_IS_BIT_SET(HAL_ADC_GetState(&hadc1),HAL_ADC_STATE_REG_EOC)){
         //读取值
       uint16_t adc= HAL_ADC_GetValue(&hadc1);
       return adc;
    }
    return 0;
}
float POWER_GET_Value(void)
{
    uint32_t sum = 0;
    float value = 0;
    for (int i = 0; i < AVERAGE_CNT; i++)
    {
      sum += POWER_GET_ADC();
    }
    value = sum/AVERAGE_CNT * 3.3/4095.0 *2.0;
    return value;
}

uint8_t POWER_GET_Capacity(void)
{
  float value = POWER_GET_Value();
  uint8_t Capacity = (value - 3.7)/(4.2-3.7) *100;
  return Capacity;
}

