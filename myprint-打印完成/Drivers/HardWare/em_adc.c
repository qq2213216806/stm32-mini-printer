#include "em_adc.h"
#include <math.h>
#include "stdio.h"
extern ADC_HandleTypeDef hadc1;

#define AVERAGE_CNT 10               //平均数

//阻值转温度
float em_temp_calculate(float Rt)
{
    float Rp = 30000; // 30k
    float T2 = 273.15 + 25;
    float Bx = 3950; // B值
    float Ka = 273.15;
    float temp = 0.0f;

    temp = 1 / (log(Rt / Rp) / Bx + 1 / T2) - Ka + 0.5;
    return temp;
}


void get_power_temp(uint8_t *power,float *temp)
{
    uint32_t p_adc = 0;
    uint32_t t_adc = 0; 

    for (int i = 0; i < AVERAGE_CNT; i++)
    {
        /*电源电压adc数据*/
        HAL_ADC_Start(&hadc1);
        if(HAL_ADC_PollForConversion(&hadc1,100)== HAL_OK)
        {
            p_adc += HAL_ADC_GetValue(&hadc1);
            printf("p_adc:%d\n",p_adc);
        }
        /*热敏电阻adc数据*/
        HAL_ADC_Start(&hadc1);
        if(HAL_ADC_PollForConversion(&hadc1,100)== HAL_OK)
        {
            t_adc += HAL_ADC_GetValue(&hadc1);
             printf("t_adc:%d\n",t_adc);
        }
        HAL_ADC_Stop(&hadc1);
    }

    p_adc = p_adc /AVERAGE_CNT;
    t_adc = t_adc /AVERAGE_CNT;

    float volts1 = p_adc*3.3 / 4096.0*2;  //电源电压
    float volts2 = t_adc*3.3 / 4096.0;   //热敏电阻电压

    //计算热敏电阻阻值
    float Rt=(volts2*10000)/(3.3-volts2);
	printf("电阻值:%f\n",Rt);
    *power =(uint8_t)((volts1 - 3.7)/(4.1-3.7) *100); //电量百分比
    *temp = em_temp_calculate(Rt);        
}
