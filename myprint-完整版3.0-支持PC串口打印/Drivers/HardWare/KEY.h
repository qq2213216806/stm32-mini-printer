#ifndef __KEY_H
#define __KEY_H
typedef enum
 {
    KEY_Lose = 0,
    KEY_Press,
    KEY_LongPrees,
    KEY_LongPreesLose,
} KEY_Value;

KEY_Value KEY_Scan(void);
#endif 
