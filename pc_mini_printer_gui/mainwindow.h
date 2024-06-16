#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QLabel>
#include <QComboBox>
#include <QPushButton>
#include <QFormLayout>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QSlider>
#include <QLineEdit>
#include <QGridLayout>
#include <QProgressBar>
#include <QSpacerItem>
#include <QGraphicsView>
#include <QSerialPort>
#include <QSerialPortInfo>
#include <QTimer>

#include <opencv2/opencv.hpp>

QT_BEGIN_NAMESPACE
namespace Ui { class MainWindow; }
QT_END_NAMESPACE


//使用opencv命名空间
class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private:
    Ui::MainWindow *ui;

    //左侧串口通信部分
    QLabel *label_serial; //文字标签:串口状态
    QComboBox *comboBox[5]; //串口下拉窗
    int port_count;
    QPushButton *btn_open_Serial; //打开串口按钮
    QFormLayout *formLayout1; //表格布局
    QLabel *label_state; //文字标签:打印机状态
    QLabel *dev_state[4];
    uint8_t dev_error;
    QFormLayout *formLayout2; //表格布局
    QVBoxLayout *s_vBoxLayout; //串口区域垂直布局
    QSpacerItem *vSpacer1;
    QSpacerItem *vSpacer2;
    QWidget *s_widget;

    //右侧图片区域
    //QLabel *Img_Source_area;  //原图预览标签
    //QLabel *Img_Preview_area; //效果图预览标签
    QGraphicsView *Img_Source_area;
    QGraphicsScene *Img_Source_Scene;
    QGraphicsView *Img_Preview_area;
    QGraphicsScene *Img_Preview_Scene;
    QLabel *label_source; //文字标签：原图
    QLabel *label_Preview; //文字标签：预览图
    QGridLayout *gridLayout;
    QWidget *widget1;

    QSlider *slider_threshold; //阈值滑动条
    QLabel  *label_threshold; //文字标签:阈值
    QHBoxLayout *hBoxlayout1;
    QWidget *widget2;

    QLabel *label_file_path; //文字标签:文件路径
    QLineEdit *file_path;//文件路径
    QPushButton *btn_Browse;//浏览按钮
    QHBoxLayout *hBoxLayout2;
    QWidget *widget3;

    QPushButton *btn_star_print; //开始打印按钮
    QProgressBar *progressBar;

    QVBoxLayout *vBoxLayout;
    QWidget *i_widget;

    //主布局
    QHBoxLayout *hBoxLayout_main;
    QWidget *Widget_main;


    //串口通信
    QSerialPort *serialPort;
    uint8_t *send_data_buffer;
    unsigned long long send_conut;
    unsigned long long send_finish_count;
    QTimer *timer1;
    QTimer *timer2;

    //opencv
    cv::Mat srcImag;
    cv::Mat binImag;
public:
    void GUI_Init(void);
    void Serial_Init(void);
    void ImageBinarization(cv::InputArray src, cv::OutputArray dst,int threshold);
    void start_send(void);
    void end_send(void);
public slots:
    void btn_open_Serial_clicked(void);
    void serialPortReadyRead(void);
    void timer1timeout(void);
    void timer2timeout(void);
    void handleSerialError(QSerialPort::SerialPortError);
    void btn_Browse_clicked(void);
    void Slide_threshold_ValueChanged(int val);
    void btn_star_print_clicked(void);
};
#endif // MAINWINDOW_H
