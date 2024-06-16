#include "mainwindow.h"
#include "ui_mainwindow.h"
#include <QMessageBox>
#include <QDir>
#include <QFile>
#include <QFileDialog>
#include <unistd.h>
#include <QDebug>
#include <iostream>

uint8_t start_cmd[4]={0xa6,0xa6,0xa6,0xa6};
uint8_t end_cmd[4] = {0xa7,0xa7,0xa7,0xa7};

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    GUI_Init();
    Serial_Init();
    std::cout<<"hello wrld"<<std::endl;
    connect(btn_open_Serial,SIGNAL(clicked()),this,SLOT(btn_open_Serial_clicked()));
    connect(serialPort, SIGNAL(readyRead()),this, SLOT(serialPortReadyRead()));
    connect(timer1,SIGNAL(timeout()),this,SLOT(timer1timeout()));
    connect(timer2,SIGNAL(timeout()),this,SLOT(timer2timeout()));
    connect(serialPort, SIGNAL(errorOccurred(QSerialPort::SerialPortError)),this, SLOT(handleSerialError(QSerialPort::SerialPortError)));
    connect(btn_Browse, SIGNAL(clicked()),this, SLOT(btn_Browse_clicked()));
    connect(slider_threshold,SIGNAL(valueChanged(int)),this,SLOT(Slide_threshold_ValueChanged(int)));
    connect(btn_star_print, SIGNAL(clicked()),this, SLOT(btn_star_print_clicked()));
}

MainWindow::~MainWindow()
{
    delete ui;
}
void MainWindow::GUI_Init()
{
    this->setFixedSize(1080,670);
   //串口区域
   btn_open_Serial = new QPushButton(tr("打开串口")); 

   QFont font;
   font.setPixelSize(28);
   label_serial = new QLabel(tr("串口配置"));
   label_serial->setFont(font);
   for(int i=0;i<5;i++)
   {
       comboBox[i] = new QComboBox();
   }
   formLayout1 = new QFormLayout();
   formLayout1->setSpacing(20);
   formLayout1->addRow(tr("端口号:"),comboBox[0]);
   formLayout1->addRow(tr("波特率:"),comboBox[1]);
   formLayout1->addRow(tr("数据位:"),comboBox[2]);
   formLayout1->addRow(tr("校验位:"),comboBox[3]);
   formLayout1->addRow(tr("停止位:"),comboBox[4]);

   label_state = new QLabel(tr("打印机状态"));
   font.setPixelSize(28);
   label_state->setFont(font);
   for (int i=0;i<4;i++)
   {
     dev_state[i] = new QLabel();
   }
   formLayout2 = new QFormLayout();
   formLayout2->addRow(tr("电量:"),dev_state[0]);
   formLayout2->addRow(tr("温度:"),dev_state[1]);
   formLayout2->addRow(tr("纸张状态:"),dev_state[2]);
   formLayout2->addRow(tr("打印状态:"),dev_state[3]);

   vSpacer1 = new QSpacerItem(10,20,QSizePolicy::Minimum,QSizePolicy::Fixed);
   vSpacer2 = new QSpacerItem(10,50,QSizePolicy::Minimum,QSizePolicy::Fixed);
   s_vBoxLayout = new QVBoxLayout();
   s_vBoxLayout->addSpacerItem(vSpacer1);
   s_vBoxLayout->addWidget(label_serial,0,Qt::AlignCenter);
   s_vBoxLayout->addLayout(formLayout1);
   s_vBoxLayout->addSpacing(30);
   s_vBoxLayout->addWidget(btn_open_Serial);
   s_vBoxLayout->addSpacing(100);
   s_vBoxLayout->addWidget(label_state,0,Qt::AlignCenter);
   s_vBoxLayout->addLayout(formLayout2);
   s_vBoxLayout->addSpacerItem(vSpacer2);

   s_widget = new QWidget();
   s_widget->setLayout(s_vBoxLayout);


  //右侧图片区域
   //Img_Source_area = new QLabel();
   //Img_Source_area->setFixedSize(200,200);
   //Img_Preview_area = new QLabel();
   //Img_Preview_area->setFixedSize(200,200);
   Img_Source_area = new QGraphicsView();
   Img_Preview_area = new QGraphicsView();
   Img_Source_area->setFixedSize(400,400);
   Img_Preview_area->setFixedSize(400,400);
   Img_Source_Scene = new QGraphicsScene(this);
   Img_Preview_Scene = new QGraphicsScene(this);
   Img_Source_area->setScene(Img_Source_Scene);
   Img_Preview_area->setScene(Img_Preview_Scene);

   label_source = new QLabel(tr("原图"));
   label_Preview = new QLabel(tr("效果图"));
   gridLayout = new QGridLayout();
   gridLayout->addWidget(Img_Source_area,0,0,Qt::AlignCenter);
   gridLayout->addWidget(Img_Preview_area,0,1,Qt::AlignCenter);
   gridLayout->addWidget(label_source,1,0,Qt::AlignCenter);
   gridLayout->addWidget(label_Preview,1,1,Qt::AlignCenter);
   widget1 = new QWidget();
   widget1->setLayout(gridLayout);

   label_threshold = new QLabel(tr("阈值:"));
   slider_threshold = new QSlider(Qt::Horizontal);
   slider_threshold->setMinimumWidth(200);
   slider_threshold->setRange(0,255);
   slider_threshold->setValue(128);
   hBoxlayout1 = new QHBoxLayout();
   hBoxlayout1->addWidget(label_threshold);
   hBoxlayout1->addWidget(slider_threshold);
   widget2 = new QWidget();
   widget2->setLayout(hBoxlayout1);

   label_file_path = new QLabel(tr("文件路径:"));
   file_path = new QLineEdit();
   file_path->setMinimumWidth(300);
   btn_Browse = new QPushButton(tr("浏览"));
   hBoxLayout2 = new QHBoxLayout();
   hBoxLayout2 ->addWidget(label_file_path);
   hBoxLayout2->addWidget(file_path);
   hBoxLayout2->addWidget(btn_Browse);
   widget3 = new QWidget();
   widget3->setLayout(hBoxLayout2);

   btn_star_print = new QPushButton(tr("开始打印"));
   btn_star_print->setFixedSize(80,30);
   btn_star_print->setEnabled(false); //默认禁用
   progressBar = new QProgressBar();
   progressBar->setMinimumWidth(300);

   vBoxLayout = new QVBoxLayout();
   vBoxLayout->addWidget(widget1,0,Qt::AlignCenter);
   vBoxLayout->addWidget(widget2,0,Qt::AlignCenter);
   vBoxLayout->addWidget(widget3,0,Qt::AlignCenter);
   vBoxLayout->addWidget(btn_star_print,0,Qt::AlignCenter);
  // vBoxLayout->addWidget(progressBar,0,Qt::AlignCenter);
   i_widget = new QWidget();
   i_widget->setLayout(vBoxLayout);

   //主布局
   hBoxLayout_main = new QHBoxLayout();
   hBoxLayout_main->addWidget(s_widget); //左侧串口
   hBoxLayout_main->addWidget(i_widget); //右侧图片
   Widget_main = new QWidget(this);
   Widget_main ->setLayout(hBoxLayout_main);
   this->setCentralWidget(Widget_main);
}
void MainWindow::Serial_Init()
{
    serialPort = new QSerialPort(this);
    timer1 = new QTimer();
    timer2 = new QTimer();
    send_conut = 0;
    send_finish_count = 0;
    send_data_buffer = NULL;
    /* 查找可用串口 */
    port_count = QSerialPortInfo::availablePorts().count(); //记录端口数，端口数不等，说明端口有更新
    foreach (const QSerialPortInfo &info,
            QSerialPortInfo::availablePorts()) {
        comboBox[0]->addItem(info.portName());
    }

    //波特率初始化
    /* QList链表，字符串类型 */
    QList <QString> list1;
    list1<<"1200"<<"2400"<<"4800"<<"9600"
       <<"19200"<<"38400"<<"57600"
      <<"115200"<<"230400"<<"460800"
     <<"921600";
    for (int i = 0; i < 11; i++) {
        comboBox[1]->addItem(list1[i]);
    }
    comboBox[1]->setCurrentIndex(7);

    //数据位初始化
    /* QList链表，字符串类型 */
    QList <QString> list2;
    list2<<"5"<<"6"<<"7"<<"8";
    for (int i = 0; i < 4; i++) {
        comboBox[2]->addItem(list2[i]);
    }
    comboBox[2]->setCurrentIndex(3);

    //校验位初始化
    /* QList链表，字符串类型 */
    QList <QString> list3;
    list3<<"None"<<"Even"<<"Odd"<<"Space"<<"Mark";
    for (int i = 0; i < 5; i++) {
        comboBox[3]->addItem(list3[i]);
    }
    comboBox[3]->setCurrentIndex(0);

    //停止位初始化
    /* QList链表，字符串类型 */
    QList <QString> list4;
    list4<<"1"<<"2";
    for (int i = 0; i < 2; i++) {
        comboBox[4]->addItem(list4[i]);
    }
    comboBox[4]->setCurrentIndex(0);


    timer1->setInterval(1000);
    timer1->start();
    timer2->setInterval(10);

}
void MainWindow::btn_open_Serial_clicked()
{
    if (btn_open_Serial->text() == "打开串口") {
        /* 设置串口名 */
        serialPort->setPortName(comboBox[0]->currentText());
        /* 设置波特率 */
        serialPort->setBaudRate(comboBox[1]->currentText().toInt());
        /* 设置数据位数 */
        switch (comboBox[2]->currentText().toInt()) {
        case 5:
            serialPort->setDataBits(QSerialPort::Data5);
            break;
        case 6:
            serialPort->setDataBits(QSerialPort::Data6);
            break;
        case 7:
            serialPort->setDataBits(QSerialPort::Data7);
            break;
        case 8:
            serialPort->setDataBits(QSerialPort::Data8);
            break;
        default: break;
        }
        /* 设置奇偶校验 */
        switch (comboBox[3]->currentIndex()) {
        case 0:
            serialPort->setParity(QSerialPort::NoParity);
            break;
        case 1:
            serialPort->setParity(QSerialPort::EvenParity);
            break;
        case 2:
            serialPort->setParity(QSerialPort::OddParity);
            break;
        case 3:
            serialPort->setParity(QSerialPort::SpaceParity);
            break;
        case 4:
            serialPort->setParity(QSerialPort::MarkParity);
            break;
        default: break;
        }
        /* 设置停止位 */
        switch (comboBox[4]->currentText().toInt()) {
        case 1:
            serialPort->setStopBits(QSerialPort::OneStop);
            break;
        case 2:
            serialPort->setStopBits(QSerialPort::TwoStop);
            break;
        default: break;
        }
        /* 设置流控制 */
        serialPort->setFlowControl(QSerialPort::NoFlowControl);
        if (!serialPort->open(QIODevice::ReadWrite))
            QMessageBox::about(NULL, "错误",
                               "串口无法打开！可能串口已经被占用！");
        else {
            for (int i = 0; i < 5; i++)
                comboBox[i]->setEnabled(false);
            btn_star_print->setEnabled(true); //使能开始打印按钮
            timer1->stop();
            btn_open_Serial->setText("关闭串口");
        }
    } else
    {
        serialPort->close();
        for (int i = 0; i < 5; i++)
        { comboBox[i]->setEnabled(true);}
        btn_star_print->setEnabled(false); //禁用开始打印按钮
        timer1->start();
        btn_open_Serial->setText("打开串口");
    }
}
void MainWindow::serialPortReadyRead()
{
    /* 接收缓冲区中读取数据 */
    QByteArray buf = serialPort->readAll();
    uint8_t *data =(uint8_t *)buf.data();
    if(data[0] == 0xA8 && data[1] == 0xA8 && data[2] == 0xA8 && data[3] == 0xA8 )
    {
        dev_state[0]->setText(QString::number(data[4]) + "%");
        dev_state[1]->setText(QString::number(data[5]) + "℃");

        if(data[6] == 1)
        {
            dev_state[2]->setText("缺纸");
        }else
        {
             dev_state[2]->setText("不缺纸");
        }
        if(data[7] == 0)
        {
            dev_state[3]->setText("空闲");
        }else
        {
            dev_state[3]->setText("打印中");
        }
        dev_error = 0;
        if(data[6] == 1 )
        {
            dev_error = 1; //缺纸错误
        }else if(data[5] > 60)
        {
            dev_error = 2;//温度过高错误
        }
    }

}
void MainWindow::timer1timeout()
{
    //端口数不等，说明端口有更新
    if(port_count != QSerialPortInfo::availablePorts().count() )
     {
        comboBox[0]->clear();
        foreach (const QSerialPortInfo &info,QSerialPortInfo::availablePorts())
        {
             comboBox[0]->addItem(info.portName());
        }
        port_count = QSerialPortInfo::availablePorts().count();
      }
     timer1->start();
}

void MainWindow::timer2timeout()
{
    if(send_finish_count >= send_conut || dev_error > 0)
    {
        //停止发送
        end_send();
        if(dev_error == 1)
        {
            QMessageBox::warning(this,tr("警告"),tr("打印异常停止:打印机缺纸！"));
        }else if(dev_error == 2)
        {
           QMessageBox::warning(this,tr("警告"),tr("打印异常停止:打印机温度过高！"));
        }
    }else
    {
        serialPort->write((char*)(send_data_buffer+send_finish_count),48);
        send_finish_count += 48;
        qDebug()<<"finish_conuynt"<<send_finish_count;
        timer2->start();
    }
}

void MainWindow::handleSerialError(QSerialPort::SerialPortError error)
{
    qDebug()<<error;
    if(error ==  QSerialPort::ResourceError)
    {
        serialPort->close();
        for (int i = 0; i < 5; i++)
        { comboBox[i]->setEnabled(true);}
        btn_star_print->setEnabled(false); //禁用开始打印按钮
        timer1->start();
        btn_open_Serial->setText("打开串口");
        QMessageBox::warning(this,tr("错误"),tr("串口异常断开！"));
    }
    if(error == QSerialPort::PermissionError)
    {
        serialPort->open(QIODevice::ReadWrite);
    }
}

void MainWindow::btn_Browse_clicked()
{
    QString curPath=QDir::currentPath();//获取当前路径
    QString fileName=QFileDialog::getOpenFileName(this,"打开文件",curPath);
    //没有选择文件则直接退出
    if(fileName.isEmpty())
    {
        return;
    }
    file_path->setText(fileName);

    /*读取原图*/
    // srcImag = cv::imread(fileName.toUtf8().data()); //不用这个是为了支持中文路径
    QFile file(fileName);
    if (file.open(QIODevice::ReadOnly))
    {
        QByteArray byteArray = file.readAll();
        std::vector<char> data(byteArray.data(), byteArray.data() + byteArray.size());
        srcImag = cv::imdecode(cv::Mat(data), CV_LOAD_IMAGE_COLOR); //等同于cv::IMREAD_COLOR,Return a 3-channel color image
        file.close();
    }

    cv::Mat tmpImag;
    // 修改大小
    double src_width = srcImag.cols;
    double src_hight = srcImag.rows;
    int dim_width = 384;
    int dim_hight = 384.0 /src_width * src_hight;
    cv::resize(srcImag,srcImag,cv::Size(dim_width,dim_hight),0,0, CV_INTER_LINEAR);
    qDebug()<<"src_w:"<<srcImag.cols<<"src_h:"<<srcImag.rows;
    //原图显示
    cv::cvtColor(srcImag,tmpImag,CV_BGR2RGB);
    QImage displayImg1 = QImage(tmpImag.data,tmpImag.cols,tmpImag.rows,tmpImag.cols*tmpImag.channels(),QImage::Format_RGB888);
    Img_Source_Scene->clear(); //清空场景
    Img_Source_Scene->setSceneRect(QRectF(0,0,tmpImag.cols,tmpImag.rows));
    Img_Source_Scene->addPixmap(QPixmap::fromImage(displayImg1));
    //二值化处理
    int threshold = slider_threshold->value();
    ImageBinarization(srcImag,binImag,threshold);
    //显示二值化后的图像
    cv::cvtColor(binImag,tmpImag,CV_GRAY2RGB);
    QImage displayImg2 = QImage(tmpImag.data,tmpImag.cols,tmpImag.rows,tmpImag.cols*tmpImag.channels(),QImage::Format_RGB888);
    Img_Preview_Scene->clear(); //清空场景
    Img_Preview_Scene->setSceneRect(QRectF(0,0,tmpImag.cols,tmpImag.rows));
    Img_Preview_Scene->addPixmap(QPixmap::fromImage(displayImg2));
}
void MainWindow::ImageBinarization(cv::InputArray src, cv::OutputArray dst,int threshold)
{
    cv::Mat tmpImage;
    //灰度化
    cv::cvtColor(src,tmpImage,cv::COLOR_BGR2GRAY);
    //二值化
    cv::threshold(tmpImage,dst,threshold,255,cv::THRESH_BINARY);
}
void MainWindow::Slide_threshold_ValueChanged(int val)
{
    (void)val;
    int threshold = slider_threshold->value();
    //没有原图则直接退出
    if(srcImag.empty())
     {
       return;
     }
    ImageBinarization(srcImag,binImag,threshold);
    //显示二值图片
    cv::Mat tmpImag;
    cv::cvtColor(binImag,tmpImag,CV_GRAY2RGB);
    QImage displayImg2 = QImage(tmpImag.data,tmpImag.cols,tmpImag.rows,tmpImag.cols*tmpImag.channels(),QImage::Format_RGB888);
    Img_Preview_Scene->clear(); //清空场景
    Img_Preview_Scene->setSceneRect(QRectF(0,0,tmpImag.cols,tmpImag.rows));
    Img_Preview_Scene->addPixmap(QPixmap::fromImage(displayImg2));
}
void MainWindow::btn_star_print_clicked()
{
    if(btn_star_print->text() == "开始打印")
    {
        if(binImag.empty())
        {
            QMessageBox::warning(this,tr("警告"),tr("没有图片数据，请先选择图片"));
            return;
        }
        qDebug()<<"binImag_w:"<<binImag.cols<<"  binImag_h:" <<binImag.rows;

        //图片数据处理 主要是把每行384个数据点压成48字节
        if(send_data_buffer != NULL)
        {
            //释放旧数据
            delete []send_data_buffer;
            send_data_buffer=NULL;
            send_conut = 0;
            send_finish_count = 0;
        }

        send_conut = binImag.rows*48;
        send_data_buffer = new unsigned char[send_conut]();

        std::cout<<_msize(send_data_buffer)<<std::endl;
        qDebug()<<"send_count:"<<send_conut;

        unsigned long long index = 0;
        for(unsigned long long i=0;i<send_conut;i++)
        {
            //printf("scr_d:%d ",send_data_buffer[i]);
            for (int j=7;j >= 0;j--)
            {
                    //数据来到这已经是 二值化图片了,255白色 0黑色
                   uint8_t  data =   binImag.data[index] == 255 ? 0:1;   //白色对打印机0;黑色对应打印机1
                  // printf("%d ",data);
                    send_data_buffer[i] |= (data<<j);
                    index++;
            }
           // printf("ret:%d ",send_data_buffer[i]);
        }

        //串口发送数据 开始打印
        if(dev_state[3]->text() =="空闲")
        {
            start_send();
        }else
        {
            QMessageBox::warning(this,tr("警告"),tr("设备打印中，请等待设备空闲"));
        }
     }else
    {
      end_send();
    }

}
void MainWindow::start_send()
{
    send_finish_count = 0;
    btn_star_print->setText("停止打印");
    serialPort->write((char*)start_cmd,sizeof (start_cmd)); //发送开始命令，告知stm32清自己的缓存区
    timer2->setInterval(10);
    timer2->start();
}
void MainWindow::end_send()
{
    timer2->stop();
    btn_star_print->setText("开始打印");
    serialPort->write((char*)end_cmd,sizeof (end_cmd));
    qDebug()<<"send_finish_count:"<<send_finish_count;

    //清空内存
    delete []send_data_buffer;
    send_data_buffer=NULL;
    send_conut = 0;
    send_finish_count = 0;
}
