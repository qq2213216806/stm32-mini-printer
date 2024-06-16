# stm32-mini-printer
基于stm32的迷你打印机

## 项目描述
基于STM32，Freertos，Hal库、Qt 的小型蓝牙热敏打印机. 外接电池供电，便携，蓝牙、pc串口连接使用.

## 项目目录
| 代码         | 功能                          |
| ------------ | ----------------------------- |
| myprint-打印完成  | 过渡版本-不完善             |
| myprint-完整版1.0 | 1.0 版本，缺点启动速度慢，约2分钟|
|myprint-完整版2.0-优化启动速度 | 启动速度优化，约1分钟|
|myprint-完整版3.0-支持PC串口打印 | 支持QT上位机打印 |
| pc_mini_printer_gui | Qt上位机源码目录 |
| miniprinter-app     | 手机app,可使用蓝牙打印(由b站up:小智学长制作) |

stm32 程序目录
**Drivers\HardWare**
| 代码         | 功能                          |
| ------------ | ----------------------------- |
| BLE.c             | 蓝牙模块驱动              |
| driver_timer.c    | 定时器驱动                |
| em_adc.c          | 电量与温度检测驱动         |
| KEY.c             | 按键驱动，可长按、短按     |
| motor.c           | 步进电机模块              |
| printer.c         | 热敏打印模块驱动          |
| serial.c          | 串口打印驱动              |   
| mytask.c          | FreeRtos多任务           |

Qt 程序目录
**pc_mini_printer_gui**
pc-mini-gui 目录是打包好的Qt程序可以直接点开使用
注：本QT程序利用了opencv库，opencv版本号3.4.6 阅读源码请自行移植

## 程序实现
stm32 程序
LED 任务: 对设备状态进行监测: 设备正常:常亮  设备打印中:慢闪  设备异常(缺纸或温度过高):快闪

按键任务：长按:步进电机运行(出纸，但不打印)   长按松:步进电机停止  短按：打印测试(打印数据已由程序固定) 

设备状态检测任务(task_dev_check) : 定时检查设备的电量、打印头温度、判断是否缺纸、设备打印状态，并将设备通过蓝牙、串口上报数据给上位机

打印任务: 利用freeRTOS的事件组、消息队列的机制,令该任务平时处于阻塞状态，当接收的打印消息大于50条时，在蓝牙/串口的接收中断里唤醒该任务开始打印。事件组唤醒任务后，会读取一行打印消息队列，接着检测设备状态是否异常（出现异常则停止本次打印，否则正常打印）
什么时候打印结束:并不依靠上位机的打印结束标志，而是判断打印消息队列是否为空，为空则说明本次打印结束。因为是边打印边接收数据，接收的数据写在消息队列尾部，而打印是读取队列头部数据，读完就删。利用消息队列的机制可以做到利用少量的RAM，打印大量数据（具体一次能打印多少行，取决打印头温度的上限，不用在考虑RAM够不的问题了）。

QT 程序
界面预览：
![alt text](<E9KL)8~YC5_WIL(3E8B]@07.png>)

界面程序好像没多少东西好讲的,基本都是依靠QT本身的控件做的界面初始化,再用qt信号与槽的机制实现相对应功能即可。
不过注意，那两个图片预览，并不是用Qlabel控件而用QGraphicsView,这样可以保持预览图片的框大小不变，还能完整的预览整个图片。

重点讲下图片数据处理部分：btn_Browse_clicked 和 btn_star_print_clicked() 大部分处理都在这两个函数里
具体原理：
利用opencv库对图片数据进行处理:读取原图，对原图片的大小修改为(384*xxx)，对修改后的图片进行二值化。
二值化后的图片每行是384个字节，而打印机一行是48个字节，所以需要384字节 压缩成 48字节后，再发送。
注：数据的发送是用QTimer来一行一行的转发的（不能直接全部数据一下子全发出去，这样做只能发4096个字节）
1.imread() 函数 读取原图
、、、
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
、、、

2.resize() 函数 修改图片大小，打印机支持打印图片的宽大小为384，高不限,所以需要修改为 384*xxx
、、、
    double src_width = srcImag.cols;
    double src_hight = srcImag.rows;
    int dim_width = 384;
    int dim_hight = 384.0 /src_width * src_hight;  //保持图片原宽高比
    cv::resize(srcImag,srcImag,cv::Size(dim_width,dim_hight),0,0, CV_INTER_LINEAR);
、、、

3.二值化处理
、、、
void MainWindow::ImageBinarization(cv::InputArray src, cv::OutputArray dst,int threshold)
{
    cv::Mat tmpImage;
    //灰度化
    cv::cvtColor(src,tmpImage,cv::COLOR_BGR2GRAY);
    //二值化
    cv::threshold(tmpImage,dst,threshold,255,cv::THRESH_BINARY);    //threshold 是那个阈值滑动条的值
}
、、、

4.384字节压缩成48字节
、、、
    //因为二值化后图片数据不是255(白)就是0(黑)，所以我们可以将每8个字节合并成一个字节
    for (int j=7;j >= 0;j--)
    {
        //数据来到这已经是 二值化图片了,255白色 0黑色
        uint8_t  data =   binImag.data[index] == 255 ? 0:1;   //白色对打印机0;黑色对应打印机1
        send_data_buffer[i] |= (data<<j);            
        index++;
    }
、、、