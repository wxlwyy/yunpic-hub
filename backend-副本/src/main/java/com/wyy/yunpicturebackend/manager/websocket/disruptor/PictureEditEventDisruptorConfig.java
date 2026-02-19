package com.wyy.yunpicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

// Spring 配置类，核心作用是：初始化一个高性能异步事件处理管道（Disruptor）
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventWorkHandler;

    /* 这里写了 destroyMethod = "shutdown" 就不用写 close()，Disruptor 内部启动了线程。如果你的 Spring Boot 应用关闭了，但没有显式关闭 Disruptor，
    那个后台线程可能还在空转，导致进程退不干净（JVM 无法退出）。原理：当 Spring 容器销毁这个 Bean 时，会自动调用 disruptor.shutdown() 方法。
    shutdown() 会等待当前环形数组里的任务全部处理完，然后再关闭线程，保证数据不丢失。 */
    @Bean(name = "pictureEditEventDisruptor", destroyMethod = "shutdown")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() { // 创建 Disruptor 实例
        /* 环形数组容量，预分配内存避免 GC 压力，= 262,144就是26万个事件槽，环形缓冲区的槽位数量，实际内存占用 = 26万 × 单个事件对象大小
        （假设 100 字节 ≈ 25MB），事件数是 2 的 N 次幂的原因：位运算加速，序号生成：Disruptor 内部有一个 Sequence（序号），
        从 0 一直往上加（0, 1, 2... 100000...）。数组下标：要把这个无限增长的序号，映射到有限的数组下标上（比如数组长度 10）。
        普通做法：index = sequence % size （取模运算）。Disruptor 做法：index = sequence & (size - 1) （按位"与"运算）。
        在 CPU 指令层面，位运算 (&) 的速度远远快于 取模运算 (%)。但这个优化成立的前提是：数组长度（size）必须是 2 的 N 次幂。*/
        int bufferSize = 1024 * 256;  
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(   
            /* 预分配事件对象池，避免运行时频繁 new，普通队列（ArrayBlockingQueue）：生产者：new Event() -> 放入队列。
            消费者：取出 Event -> 处理 -> 丢弃。后果：由于你的请求量巨大（比如协同编辑时的鼠标移动），JVM 堆内存里会疯狂地创建和销毁数百万个小对象，
            导致 GC（垃圾回收）频繁触发，引起系统卡顿（STW）。 启动时：Disruptor 会根据你传的 PictureEditEvent::new，一次性把 bufferSize（26万个）
            事件对象全部 new 出来，填满整个数组。运行时：生产者要发消息？-> 从数组里领一个旧对象。修改它的属性（Setters）。消费者处理完。
            对象不销毁，留在数组里等待下一次被复用。结果：整个运行过程中，没有新的 Event 对象产生，也没有对象被销毁。GC 压力几乎为零。*/
            PictureEditEvent::new,   
            bufferSize,
            /*创建一个线程工厂，专门用于生成 Disruptor 消费者线程。生成的线程名会是：pictureEditEventDisruptor-1, pictureEditEventDisruptor-2...
            不设置的后果：线程名是 pool-2-thread-1 这种无意义名字*/
            ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        /* 设置消费者（启用 Worker Pool 模式，会创建多个消费者线程），
        Disruptor 有两种消费模式：handleEventsWith(EventHandler)：广播/独立消费。如果有多个 Handler，每个 Handler 都会收到同一条消息副本。
        handleEventsWithWorkerPool(WorkHandler)：竞争消费（Worker Pool）。如果有多个 Handler，消息会被分发（一条消息只能被一个 Handler 抢到）。
        传入了一个单例的 pictureEditEventWorkHandler。就构建了一个 “单消费者” 模型。 */
        disruptor.handleEventsWithWorkerPool(pictureEditEventWorkHandler);
        // 开启 disruptor（初始化环形缓冲区、消费者线程池，进入就绪状态）
        disruptor.start();  
        return disruptor;
    }
}