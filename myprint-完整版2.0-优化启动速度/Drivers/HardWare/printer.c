#include "printer.h"
#include "spi.h"
#include "gpio.h"
#include "driver_timer.h"
#include "motor.h"



static void digitalWrite(int pin,int PinState){
	if(pin == PIN_STB1){
		HAL_GPIO_WritePin(STB1_GPIO_Port,STB1_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_STB2){
		HAL_GPIO_WritePin(STB2_GPIO_Port,STB2_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_STB3){
		HAL_GPIO_WritePin(STB3_GPIO_Port,STB3_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_STB4){
		HAL_GPIO_WritePin(STB4_GPIO_Port,STB4_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_STB5){
		HAL_GPIO_WritePin(STB5_GPIO_Port,STB5_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_STB6){
		HAL_GPIO_WritePin(STB6_GPIO_Port,STB6_Pin,(GPIO_PinState)PinState);
	}else if(pin == PIN_LAT){
		HAL_GPIO_WritePin(LAT_GPIO_Port,LAT_Pin,(GPIO_PinState)PinState);
	}
}

static void digitalWrite_vhen(int pin,int PinState){
	HAL_GPIO_WritePin(VH_EN_GPIO_Port,VH_EN_Pin,(GPIO_PinState)PinState);
}

/*所有通道失能*/
void printer_stb_idle(void)
{
	digitalWrite(PIN_STB1,LOW);
	digitalWrite(PIN_STB2,LOW);
	digitalWrite(PIN_STB3,LOW);
	digitalWrite(PIN_STB4,LOW);
	digitalWrite(PIN_STB5,LOW);
	digitalWrite(PIN_STB6,LOW);
}

/*打印机初始化*/
void printer_init(void)
{
	digitalWrite_vhen(PIN_VHEN,LOW);
	digitalWrite(PIN_LAT,HIGH);
	printer_stb_idle();
}

/*开始打印准备*/
void printer_start(void)
{
	digitalWrite(PIN_LAT,HIGH);
	printer_stb_idle();
	digitalWrite_vhen(PIN_VHEN,HIGH); //开始加热
}

/*结束打印*/
void printer_stop(void)
{
	digitalWrite(PIN_LAT,HIGH);
	printer_stb_idle();
	digitalWrite_vhen(PIN_VHEN,LOW); //停止加热
}

/*发送一行数据  48字节*/
static void send_one_line_data(uint8_t *data,int len)
{
	HAL_SPI_Transmit(&hspi1,data,len,HAL_MAX_DELAY);
	/*数据锁存信号*/
	digitalWrite(PIN_LAT,LOW);
	udelay(LAT_TIME);
	digitalWrite(PIN_LAT,HIGH);
}

/*打打印指定通道*/
static void run_stb(uint8_t num)
{
	 switch (num)
    {
    case 0:
        digitalWrite(PIN_STB3, 1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB3, 0);
        udelay(PRINT_END_TIME);
        break;
    case 1:
        digitalWrite(PIN_STB2,1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB2, 0);
        udelay(PRINT_END_TIME);
        break;
    case 2:
        digitalWrite(PIN_STB1, 1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB1, 0);
        udelay(PRINT_END_TIME);
        break;
    case 3:
        digitalWrite(PIN_STB6, 1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB6, 0);
        udelay(PRINT_END_TIME);
        break;
    case 4:
        digitalWrite(PIN_STB5, 1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB5, 0);
        udelay(PRINT_END_TIME);
        break;
    case 5:
        digitalWrite(PIN_STB4, 1);
        udelay(PRINT_TIME);
        digitalWrite(PIN_STB4, 0);
        udelay(PRINT_END_TIME);
        break;
    default:
        break;
    }
}
/*打印一行数据*/
void printer_printing_one_line_data(uint8_t *data)
{
	/*发送一行数据*/
	send_one_line_data(data,ONE_LINE_LEN); 
	/*打印一行  实测是一行而非4步一行*/
	motor_run_step(1);
	for (int index = 0; index < 6; index++)
	{
		run_stb(index);
	}
	//motor_run_step(3);
}


void printer_start_printing(uint8_t *data,int len)
{
	uint8_t *ptr = data;
	/*打印准备开始,开始加热*/
	printer_start();

	for(int i=0;i < len;i += ONE_LINE_LEN )
	{
		/*发送一行数据*/
		send_one_line_data(ptr,ONE_LINE_LEN); 

		/*打印一行*/
		//motor_run_step(1);
		for (int index = 0; index < 6; index++)
		{
			motor_run_step(4);
			run_stb(index);
		}
		//motor_run_step(3);

		ptr += ONE_LINE_LEN;  //下一行数据的偏移地址
	}
		
	/*结束打印*/
	printer_stop();
	motor_stop();
}


void printer_stb_test(void)
{
	uint8_t data[48*5];
	for (int i = 0; i< 48*5; i++)
	{
		data[i] = 0X55;
	}
	printer_start_printing(data,48*5);
}
