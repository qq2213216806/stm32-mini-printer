#include "motor.h"
#include "gpio.h"


static void digitalWrite(int pin,int PinState){
	if(pin == PIN_MOTOR_AP){
		HAL_GPIO_WritePin(MOTOR_IN1_GPIO_Port,MOTOR_IN1_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_MOTOR_AM){
		HAL_GPIO_WritePin(MOTOR_IN2_GPIO_Port,MOTOR_IN2_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_MOTOR_BP){
		HAL_GPIO_WritePin(MOTOR_IN3_GPIO_Port,MOTOR_IN3_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_MOTOR_BM){
		HAL_GPIO_WritePin(MOTOR_IN4_GPIO_Port,MOTOR_IN4_Pin,(GPIO_PinState)PinState);
	}
}

void motor_init(void)
{
    digitalWrite(PIN_MOTOR_AP,0);
    digitalWrite(PIN_MOTOR_AM,0);
    digitalWrite(PIN_MOTOR_BP,0);
    digitalWrite(PIN_MOTOR_BM,0);
}





/*八拍 一步时序
    A-  A+  B-  B+
    0   1   1   0
    0   0   1   0
    1   0   1   0
    1   0   0   0
    1   0   0   1
    0   0   0   1
    0   1   0   1
    0   1   0   0
*/
uint8_t motor_table[8][4] =
    {
        {0, 1, 1, 0},
        {0, 0, 1, 0},
        {1, 0, 1, 0},
        {1, 0, 0, 0},
        {1, 0, 0, 1},
        {0, 0, 0, 1},
        {0, 1, 0, 1},
        {0, 1, 0, 0}
    };  //步序表

//电机运行step步
void motor_run_step(uint8_t step)
{
    for(uint8_t i=0;i < step;i++)
    {
        for (uint8_t j = 0; j < 8; j++)
        {
            digitalWrite(PIN_MOTOR_AP,motor_table[j][0]);
            digitalWrite(PIN_MOTOR_AM,motor_table[j][1]);
            digitalWrite(PIN_MOTOR_BP,motor_table[j][2]);
            digitalWrite(PIN_MOTOR_BM,motor_table[j][3]);
            HAL_Delay(2);  //延迟4ms
        }
        
    }
}

void motor_stop(void)
{
    digitalWrite(PIN_MOTOR_AP,0);
    digitalWrite(PIN_MOTOR_AM,0);
    digitalWrite(PIN_MOTOR_BP,0);
    digitalWrite(PIN_MOTOR_BM,0);
}
