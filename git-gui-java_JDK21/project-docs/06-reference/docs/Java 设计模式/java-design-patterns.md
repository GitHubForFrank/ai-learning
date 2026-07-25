# Java 设计模式大全

> 设计模式（Design Pattern）是前辈们对代码开发经验的总结，是解决特定问题的一系列套路。它不是语法规定，而是一套用来提高代码可复用性、可维护性、可读性、稳健性以及安全性的解决方案。

> **Token 估算**：约 36K tokens

---

## 目录

- [Java 设计模式大全](#java-设计模式大全)
  - [目录](#目录)
  - [1. 创建型模式（Creational Patterns）](#1-创建型模式creational-patterns)
    - [1.1 单例模式（Singleton）](#11-单例模式singleton)
      - [意图](#意图)
      - [适用场景](#适用场景)
      - [实现方式](#实现方式)
      - [案例：数据库连接池](#案例数据库连接池)
      - [破坏单例的场景与防范](#破坏单例的场景与防范)
    - [1.2 工厂方法模式（Factory Method）](#12-工厂方法模式factory-method)
      - [意图](#意图-1)
      - [适用场景](#适用场景-1)
      - [基本结构](#基本结构)
      - [案例：日志记录器工厂](#案例日志记录器工厂)
      - [JDK 中的工厂方法](#jdk-中的工厂方法)
    - [1.3 抽象工厂模式（Abstract Factory）](#13-抽象工厂模式abstract-factory)
      - [意图](#意图-2)
      - [与工厂方法的区别](#与工厂方法的区别)
      - [基本结构](#基本结构-1)
      - [案例：跨数据库 DAO 工厂](#案例跨数据库-dao-工厂)
    - [1.4 建造者模式（Builder）](#14-建造者模式builder)
      - [意图](#意图-3)
      - [适用场景](#适用场景-2)
      - [基本结构](#基本结构-2)
      - [案例：HTTP 请求构建器](#案例http-请求构建器)
    - [1.5 原型模式（Prototype）](#15-原型模式prototype)
      - [意图](#意图-4)
      - [适用场景](#适用场景-3)
      - [浅拷贝 vs 深拷贝](#浅拷贝-vs-深拷贝)
      - [基本实现](#基本实现)
      - [案例：原型注册表](#案例原型注册表)
  - [2. 结构型模式（Structural Patterns）](#2-结构型模式structural-patterns)
    - [2.1 适配器模式（Adapter）](#21-适配器模式adapter)
      - [意图](#意图-5)
      - [适用场景](#适用场景-4)
      - [两种实现方式](#两种实现方式)
      - [案例：支付网关适配](#案例支付网关适配)
      - [JDK 中的适配器](#jdk-中的适配器)
    - [2.2 桥接模式（Bridge）](#22-桥接模式bridge)
      - [意图](#意图-6)
      - [适用场景](#适用场景-5)
      - [基本结构](#基本结构-3)
      - [案例：消息发送通道](#案例消息发送通道)
    - [2.3 组合模式（Composite）](#23-组合模式composite)
      - [意图](#意图-7)
      - [适用场景](#适用场景-6)
      - [基本结构](#基本结构-4)
      - [案例：文件系统](#案例文件系统)
    - [2.4 装饰器模式（Decorator）](#24-装饰器模式decorator)
      - [意图](#意图-8)
      - [适用场景](#适用场景-7)
      - [基本结构](#基本结构-5)
      - [案例：数据流加密处理](#案例数据流加密处理)
      - [JDK 中的装饰器](#jdk-中的装饰器)
    - [2.5 外观模式（Facade）](#25-外观模式facade)
      - [意图](#意图-9)
      - [适用场景](#适用场景-8)
      - [基本结构](#基本结构-6)
      - [案例：订单服务外观](#案例订单服务外观)
    - [2.6 享元模式（Flyweight）](#26-享元模式flyweight)
      - [意图](#意图-10)
      - [核心概念](#核心概念)
      - [适用场景](#适用场景-9)
      - [基本结构](#基本结构-7)
      - [案例：在线游戏中的子弹](#案例在线游戏中的子弹)
      - [JDK 中的享元](#jdk-中的享元)
    - [2.7 代理模式（Proxy）](#27-代理模式proxy)
      - [意图](#意图-11)
      - [代理的分类](#代理的分类)
      - [静态代理](#静态代理)
      - [动态代理（JDK）](#动态代理jdk)
      - [CGLIB 动态代理](#cglib-动态代理)
      - [案例：缓存代理](#案例缓存代理)
      - [Spring AOP 与代理](#spring-aop-与代理)
  - [3. 行为型模式（Behavioral Patterns）](#3-行为型模式behavioral-patterns)
    - [3.1 责任链模式（Chain of Responsibility）](#31-责任链模式chain-of-responsibility)
      - [意图](#意图-12)
      - [适用场景](#适用场景-10)
      - [基本结构](#基本结构-8)
      - [案例：Web 过滤器链](#案例web-过滤器链)
      - [Servlet Filter 就是典型的责任链模式](#servlet-filter-就是典型的责任链模式)
    - [3.2 命令模式（Command）](#32-命令模式command)
      - [意图](#意图-13)
      - [适用场景](#适用场景-11)
      - [基本结构](#基本结构-9)
      - [案例：文本编辑器操作](#案例文本编辑器操作)
      - [JDK 中的命令模式](#jdk-中的命令模式)
    - [3.3 解释器模式（Interpreter）](#33-解释器模式interpreter)
      - [意图](#意图-14)
      - [适用场景](#适用场景-12)
      - [案例：四则运算表达式求值](#案例四则运算表达式求值)
    - [3.4 迭代器模式（Iterator）](#34-迭代器模式iterator)
      - [意图](#意图-15)
      - [适用场景](#适用场景-13)
      - [JDK 中的迭代器](#jdk-中的迭代器)
      - [案例：自定义集合的迭代器](#案例自定义集合的迭代器)
    - [3.5 中介者模式（Mediator）](#35-中介者模式mediator)
      - [意图](#意图-16)
      - [适用场景](#适用场景-14)
      - [案例：聊天室](#案例聊天室)
      - [案例：微服务中的事件总线](#案例微服务中的事件总线)
    - [3.6 备忘录模式（Memento）](#36-备忘录模式memento)
      - [意图](#意图-17)
      - [适用场景](#适用场景-15)
      - [案例：游戏存档](#案例游戏存档)
    - [3.7 观察者模式（Observer）](#37-观察者模式observer)
      - [意图](#意图-18)
      - [适用场景](#适用场景-16)
      - [基本结构](#基本结构-10)
      - [案例：Spring 事件监听](#案例spring-事件监听)
      - [JDK 中的观察者](#jdk-中的观察者)
    - [3.8 状态模式（State）](#38-状态模式state)
      - [意图](#意图-19)
      - [适用场景](#适用场景-17)
      - [基本结构](#基本结构-11)
      - [案例：订单状态机](#案例订单状态机)
      - [状态模式 vs 策略模式](#状态模式-vs-策略模式)
    - [3.9 策略模式（Strategy）](#39-策略模式strategy)
      - [意图](#意图-20)
      - [适用场景](#适用场景-18)
      - [基本结构](#基本结构-12)
      - [案例：支付策略](#案例支付策略)
      - [案例：参数校验策略](#案例参数校验策略)
      - [JDK 中的策略模式](#jdk-中的策略模式)
    - [3.10 模板方法模式（Template Method）](#310-模板方法模式template-method)
      - [意图](#意图-21)
      - [适用场景](#适用场景-19)
      - [基本结构](#基本结构-13)
      - [案例：JdbcTemplate 模拟](#案例jdbctemplate-模拟)
      - [JDK / 框架中的模板方法](#jdk--框架中的模板方法)
    - [3.11 访问者模式（Visitor）](#311-访问者模式visitor)
      - [意图](#意图-22)
      - [适用场景](#适用场景-20)
      - [基本结构](#基本结构-14)
      - [案例：AST（抽象语法树）遍历](#案例ast抽象语法树遍历)
      - [访问者模式优缺点](#访问者模式优缺点)
  - [4. 设计模式选用原则](#4-设计模式选用原则)
    - [4.1 六大设计原则](#41-六大设计原则)
    - [4.2 设计模式选择指南](#42-设计模式选择指南)
      - [按目标分类速查](#按目标分类速查)
      - [常见反模式](#常见反模式)
    - [4.3 模式之间的关系](#43-模式之间的关系)
    - [4.4 实践建议](#44-实践建议)

---

## 1. 创建型模式（Creational Patterns）

创建型模式关注对象的创建过程，通过将创建逻辑与使用逻辑分离，使得系统在创建对象时更加灵活。

---

### 1.1 单例模式（Singleton）

#### 意图

确保一个类只有一个实例，并提供一个全局访问点。

#### 适用场景

- 需要全局唯一的资源，如配置管理器、连接池、日志记录器
- 需要控制共享资源的访问

#### 实现方式

**1. 饿汉式（线程安全，推荐）**

```java
public class EagerSingleton {

    private static final EagerSingleton INSTANCE = new EagerSingleton();

    private EagerSingleton() {
    }

    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

**2. 双重检查锁定（懒汉式，线程安全）**

```java
public class LazySingleton {

    private static volatile LazySingleton instance;

    private LazySingleton() {
    }

    public static LazySingleton getInstance() {
        if (instance == null) {
            synchronized (LazySingleton.class) {
                if (instance == null) {
                    instance = new LazySingleton();
                }
            }
        }
        return instance;
    }
}
```

**3. 静态内部类（推荐）**

```java
public class InnerClassSingleton {

    private InnerClassSingleton() {
    }

    private static class Holder {
        private static final InnerClassSingleton INSTANCE = new InnerClassSingleton();
    }

    public static InnerClassSingleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

**4. 枚举（最安全）**

```java
public enum EnumSingleton {
    INSTANCE;

    public void doSomething() {
        System.out.println("执行单例逻辑");
    }
}
```

#### 案例：数据库连接池

```java
public class ConnectionPool {

    private static final ConnectionPool INSTANCE = new ConnectionPool();

    private final List<Connection> pool = new ArrayList<>(10);

    private ConnectionPool() {
        for (int i = 0; i < 10; i++) {
            pool.add(createConnection());
        }
    }

    public static ConnectionPool getInstance() {
        return INSTANCE;
    }

    public synchronized Connection getConnection() {
        while (pool.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return pool.remove(pool.size() - 1);
    }

    public synchronized void releaseConnection(Connection conn) {
        pool.add(conn);
        notifyAll();
    }

    private Connection createConnection() {
        // 实际创建数据库连接
        return null;
    }
}
```

#### 破坏单例的场景与防范

| 破坏方式 | 防范措施 |
|---------|---------|
| 反射调用私有构造器 | 在构造器中判断实例是否已存在，若存在则抛异常 |
| 序列化/反序列化 | 添加 `readResolve()` 方法返回现有实例 |
| 克隆 | 重写 `clone()` 方法抛异常，或返回现有实例 |

---

### 1.2 工厂方法模式（Factory Method）

#### 意图

定义一个创建对象的接口，但让子类决定实例化哪个类。工厂方法使一个类的实例化延迟到其子类。

#### 适用场景

- 客户端不知道它所需要的对象的类型
- 一个类希望通过其子类来指定创建哪个对象

#### 基本结构

```java
// 产品接口
interface Product {
    void use();
}

// 具体产品 A
class ConcreteProductA implements Product {
    @Override
    public void use() {
        System.out.println("使用产品 A");
    }
}

// 具体产品 B
class ConcreteProductB implements Product {
    @Override
    public void use() {
        System.out.println("使用产品 B");
    }
}

// 抽象工厂
abstract class Creator {
    abstract Product createProduct();

    public void doSomething() {
        Product product = createProduct();
        product.use();
    }
}

// 具体工厂 A
class ConcreteCreatorA extends Creator {
    @Override
    Product createProduct() {
        return new ConcreteProductA();
    }
}

// 具体工厂 B
class ConcreteCreatorB extends Creator {
    @Override
    Product createProduct() {
        return new ConcreteProductB();
    }
}
```

#### 案例：日志记录器工厂

```java
interface Logger {
    void log(String message);
}

class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("[Console] " + message);
    }
}

class FileLogger implements Logger {
    @Override
    public void log(String message) {
        // 写入文件
        System.out.println("[File] " + message);
    }
}

class RemoteLogger implements Logger {
    @Override
    public void log(String message) {
        // 发送到远程服务器
        System.out.println("[Remote] " + message);
    }
}

abstract class LoggerFactory {
    abstract Logger createLogger();

    public void writeLog(String message) {
        Logger logger = createLogger();
        logger.log(message);
    }
}

class ConsoleLoggerFactory extends LoggerFactory {
    @Override
    Logger createLogger() {
        return new ConsoleLogger();
    }
}

class FileLoggerFactory extends LoggerFactory {
    @Override
    Logger createLogger() {
        return new FileLogger();
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        LoggerFactory factory = new ConsoleLoggerFactory();
        factory.writeLog("用户登录成功");
    }
}
```

#### JDK 中的工厂方法

- `java.util.Calendar.getInstance()`
- `java.util.ResourceBundle.getBundle()`
- `java.text.NumberFormat.getInstance()`
- `java.util.concurrent.Executors` 中的各种 `newXxxPool()`

---

### 1.3 抽象工厂模式（Abstract Factory）

#### 意图

提供一个创建一系列相关或相互依赖对象的接口，而无需指定它们具体的类。

#### 与工厂方法的区别

| 维度 | 工厂方法 | 抽象工厂 |
|------|---------|---------|
| 产品数量 | 单一产品 | 产品族（多个相关产品） |
| 扩展方向 | 增加产品类型容易 | 增加产品族容易 |
| 复杂度 | 较低 | 较高 |

#### 基本结构

```java
// 产品族 1
interface Button {
    void render();
}

interface TextField {
    void render();
}

// Windows 风格产品
class WindowsButton implements Button {
    @Override
    public void render() {
        System.out.println("渲染 Windows 风格按钮");
    }
}

class WindowsTextField implements TextField {
    @Override
    public void render() {
        System.out.println("渲染 Windows 风格文本框");
    }
}

// Mac 风格产品
class MacButton implements Button {
    @Override
    public void render() {
        System.out.println("渲染 Mac 风格按钮");
    }
}

class MacTextField implements TextField {
    @Override
    public void render() {
        System.out.println("渲染 Mac 风格文本框");
    }
}

// 抽象工厂
interface GUIFactory {
    Button createButton();
    TextField createTextField();
}

// Windows 工厂
class WindowsFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
}

// Mac 工厂
class MacFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public TextField createTextField() {
        return new MacTextField();
    }
}

// 客户端代码
class Application {
    private final Button button;
    private final TextField textField;

    public Application(GUIFactory factory) {
        button = factory.createButton();
        textField = factory.createTextField();
    }

    public void render() {
        button.render();
        textField.render();
    }
}
```

#### 案例：跨数据库 DAO 工厂

```java
// 产品族：UserDAO 和 OrderDAO
interface UserDAO {
    void insert(User user);
    User findById(Long id);
}

interface OrderDAO {
    void insert(Order order);
    List<Order> findByUserId(Long userId);
}

// MySQL 实现
class MySQLUserDAO implements UserDAO {
    @Override
    public void insert(User user) {
        System.out.println("MySQL: 插入用户 " + user.getName());
    }

    @Override
    public User findById(Long id) {
        System.out.println("MySQL: 查询用户 id=" + id);
        return new User(id, "张三");
    }
}

class MySQLOrderDAO implements OrderDAO {
    @Override
    public void insert(Order order) {
        System.out.println("MySQL: 插入订单 " + order.getId());
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        System.out.println("MySQL: 查询用户 " + userId + " 的订单");
        return new ArrayList<>();
    }
}

// PostgreSQL 实现
class PgUserDAO implements UserDAO {
    @Override
    public void insert(User user) {
        System.out.println("PostgreSQL: 插入用户 " + user.getName());
    }

    @Override
    public User findById(Long id) {
        System.out.println("PostgreSQL: 查询用户 id=" + id);
        return new User(id, "张三");
    }
}

class PgOrderDAO implements OrderDAO {
    @Override
    public void insert(Order order) {
        System.out.println("PostgreSQL: 插入订单 " + order.getId());
    }

    @Override
    public List<Order> findByUserId(Long userId) {
        System.out.println("PostgreSQL: 查询用户 " + userId + " 的订单");
        return new ArrayList<>();
    }
}

// 抽象 DAO 工厂
interface DAOFactory {
    UserDAO createUserDAO();
    OrderDAO createOrderDAO();
}

class MySQLDAOFactory implements DAOFactory {
    @Override
    public UserDAO createUserDAO() {
        return new MySQLUserDAO();
    }

    @Override
    public OrderDAO createOrderDAO() {
        return new MySQLOrderDAO();
    }
}

class PgDAOFactory implements DAOFactory {
    @Override
    public UserDAO createUserDAO() {
        return new PgUserDAO();
    }

    @Override
    public OrderDAO createOrderDAO() {
        return new PgOrderDAO();
    }
}
```

---

### 1.4 建造者模式（Builder）

#### 意图

将一个复杂对象的构建与其表示分离，使得同样的构建过程可以创建不同的表示。

#### 适用场景

- 需要生成的对象具有复杂的内部结构，包含多个组成部分
- 需要生成的对象内部属性存在相互依赖关系
- 对象的构建过程需要分步进行

#### 基本结构

```java
public class Computer {

    // 必选参数
    private final String cpu;
    private final String ram;

    // 可选参数
    private final String gpu;
    private final String storage;
    private final String os;

    private Computer(Builder builder) {
        this.cpu = builder.cpu;
        this.ram = builder.ram;
        this.gpu = builder.gpu;
        this.storage = builder.storage;
        this.os = builder.os;
    }

    @Override
    public String toString() {
        return "Computer{" +
                "cpu='" + cpu + '\'' +
                ", ram='" + ram + '\'' +
                ", gpu='" + gpu + '\'' +
                ", storage='" + storage + '\'' +
                ", os='" + os + '\'' +
                '}';
    }

    public static class Builder {
        // 必选参数
        private final String cpu;
        private final String ram;

        // 可选参数
        private String gpu = "集成显卡";
        private String storage = "256GB SSD";
        private String os = "Windows 11";

        public Builder(String cpu, String ram) {
            this.cpu = cpu;
            this.ram = ram;
        }

        public Builder gpu(String gpu) {
            this.gpu = gpu;
            return this;
        }

        public Builder storage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder os(String os) {
            this.os = os;
            return this;
        }

        public Computer build() {
            return new Computer(this);
        }
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Computer pc = new Computer.Builder("Intel i9", "32GB")
                .gpu("NVIDIA RTX 4090")
                .storage("1TB NVMe SSD")
                .os("Ubuntu 24.04")
                .build();

        Computer basicPc = new Computer.Builder("Intel i5", "16GB")
                .build();

        System.out.println(pc);
        System.out.println(basicPc);
    }
}
```

#### 案例：HTTP 请求构建器

```java
public class HttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final int connectTimeout;
    private final int readTimeout;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }

    public static class Builder {
        private final String method;
        private final String url;
        private final Map<String, String> headers = new HashMap<>();
        private String body = "";
        private int connectTimeout = 5000;
        private int readTimeout = 10000;

        public Builder(String method, String url) {
            this.method = method;
            this.url = url;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder connectTimeout(int timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(int timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}

// 使用
HttpRequest request = new HttpRequest.Builder("POST", "https://api.example.com/users")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer token123")
        .body("{\"name\":\"张三\"}")
        .connectTimeout(3000)
        .build();
```

---

### 1.5 原型模式（Prototype）

#### 意图

用原型实例指定创建对象的种类，并且通过拷贝这些原型来创建新的对象。

#### 适用场景

- 创建对象的成本较大（如需要复杂计算或数据库读取）
- 需要大量相似对象，且它们的差别仅在于几个属性
- 避免使用复杂的工厂类层次结构

#### 浅拷贝 vs 深拷贝

| 拷贝方式 | 说明 | 引用类型字段 |
|---------|------|-------------|
| 浅拷贝 | 仅复制基本类型字段的值 | 复制引用地址，共享同一对象 |
| 深拷贝 | 递归复制所有字段 | 创建新的对象副本 |

#### 基本实现

```java
public class Document implements Cloneable {

    private String title;
    private String content;
    private Author author; // 引用类型
    private List<String> tags;

    public Document(String title, String content, Author author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.tags = new ArrayList<>();
    }

    // 浅拷贝
    @Override
    public Document clone() {
        try {
            return (Document) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    // 深拷贝
    public Document deepClone() {
        Document clone = this.clone();
        clone.author = new Author(this.author.getName()); // 复制引用对象
        clone.tags = new ArrayList<>(this.tags);            // 复制集合
        return clone;
    }

    // 序列化方式深拷贝
    public Document deepCloneBySerialization() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (Document) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("深拷贝失败", e);
        }
    }

    // getters & setters ...
}

class Author implements Cloneable {
    private String name;

    public Author(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}
```

#### 案例：原型注册表

```java
public class PrototypeRegistry {

    private final Map<String, Document> prototypes = new HashMap<>();

    public void register(String key, Document prototype) {
        prototypes.put(key, prototype);
    }

    public Document create(String key) {
        Document prototype = prototypes.get(key);
        if (prototype == null) {
            throw new IllegalArgumentException("未注册的原型: " + key);
        }
        return prototype.deepClone();
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        PrototypeRegistry registry = new PrototypeRegistry();

        Document reportTemplate = new Document("周报模板", "本周工作总结...", new Author("系统"));
        reportTemplate.getTags().add("周报");
        registry.register("weekly_report", reportTemplate);

        // 基于模板创建新文档
        Document myReport = registry.create("weekly_report");
        myReport.setTitle("张三-2026年第21周周报");
        myReport.setContent("本周完成了用户模块开发...");
    }
}
```

---

## 2. 结构型模式（Structural Patterns）

结构型模式关注类和对象的组合，通过继承或组合来形成更大的结构。

---

### 2.1 适配器模式（Adapter）

#### 意图

将一个类的接口转换成客户端期望的另一个接口，使原本接口不兼容的类可以协同工作。

#### 适用场景

- 需要使用一个现有类，但其接口与需求不匹配
- 希望创建一个可复用的类，该类可以与多个不相关的类或不可预见的类协同工作
- 需要对多个已有子类进行适配，而不想对每个子类都实现一个新的适配器

#### 两种实现方式

**类适配器（继承）**

```java
// 已有接口（目标接口）
interface Target {
    void request();
}

// 已有类（被适配者）
class Adaptee {
    public void specificRequest() {
        System.out.println("被适配者的特殊请求");
    }
}

// 类适配器
class ClassAdapter extends Adaptee implements Target {
    @Override
    public void request() {
        // 将 request() 转换为 specificRequest()
        super.specificRequest();
    }
}
```

**对象适配器（组合，推荐）**

```java
// 对象适配器
class ObjectAdapter implements Target {

    private final Adaptee adaptee;

    public ObjectAdapter(Adaptee adaptee) {
        this.adaptee = adaptee;
    }

    @Override
    public void request() {
        adaptee.specificRequest();
    }
}
```

#### 案例：支付网关适配

```java
// 统一的支付接口
interface PaymentGateway {
    PaymentResult pay(PaymentRequest request);
}

// 支付宝 API（模拟已有接口）
class AlipayAPI {
    public String alipayPay(String orderId, double amount) {
        System.out.println("支付宝支付: " + orderId + " " + amount);
        return "SUCCESS";
    }
}

// 微信支付 API（模拟已有接口）
class WechatPayAPI {
    public boolean wechatPay(String transactionId, int totalFee) {
        System.out.println("微信支付: " + transactionId + " " + totalFee);
        return true;
    }
}

class PaymentRequest {
    private String orderId;
    private double amount;
    // getters & setters
}

class PaymentResult {
    private boolean success;
    private String message;
    // getters & setters
}

// 支付宝适配器
class AlipayAdapter implements PaymentGateway {

    private final AlipayAPI alipayAPI = new AlipayAPI();

    @Override
    public PaymentResult pay(PaymentRequest request) {
        String result = alipayAPI.alipayPay(request.getOrderId(), request.getAmount());

        PaymentResult paymentResult = new PaymentResult();
        paymentResult.setSuccess("SUCCESS".equals(result));
        paymentResult.setMessage(result);
        return paymentResult;
    }
}

// 微信支付适配器
class WechatPayAdapter implements PaymentGateway {

    private final WechatPayAPI wechatPayAPI = new WechatPayAPI();

    @Override
    public PaymentResult pay(PaymentRequest request) {
        // 金额单位转换：元 → 分
        int totalFee = (int) (request.getAmount() * 100);
        boolean result = wechatPayAPI.wechatPay(request.getOrderId(), totalFee);

        PaymentResult paymentResult = new PaymentResult();
        paymentResult.setSuccess(result);
        paymentResult.setMessage(result ? "SUCCESS" : "FAILED");
        return paymentResult;
    }
}
```

#### JDK 中的适配器

- `java.io.InputStreamReader` / `OutputStreamWriter`：字节流 → 字符流
- `java.util.Arrays.asList()`：数组 → List

---

### 2.2 桥接模式（Bridge）

#### 意图

将抽象部分与它的实现部分分离，使它们都可以独立地变化。

#### 适用场景

- 避免抽象与实现之间的永久绑定
- 抽象和实现都应通过子类化进行扩展
- 对实现部分的修改不应影响客户端

#### 基本结构

```java
// 实现部分：颜色
interface Color {
    String fill();
}

class Red implements Color {
    @Override
    public String fill() {
        return "红色";
    }
}

class Blue implements Color {
    @Override
    public String fill() {
        return "蓝色";
    }
}

class Green implements Color {
    @Override
    public String fill() {
        return "绿色";
    }
}

// 抽象部分：形状
abstract class Shape {
    protected Color color;

    public Shape(Color color) {
        this.color = color;
    }

    abstract void draw();
}

class Circle extends Shape {
    public Circle(Color color) {
        super(color);
    }

    @Override
    public void draw() {
        System.out.println("绘制" + color.fill() + "的圆形");
    }
}

class Rectangle extends Shape {
    public Rectangle(Color color) {
        super(color);
    }

    @Override
    public void draw() {
        System.out.println("绘制" + color.fill() + "的矩形");
    }
}

class Triangle extends Shape {
    public Triangle(Color color) {
        super(color);
    }

    @Override
    public void draw() {
        System.out.println("绘制" + color.fill() + "的三角形");
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Shape redCircle = new Circle(new Red());
        Shape blueRect = new Rectangle(new Blue());
        Shape greenTriangle = new Triangle(new Green());

        redCircle.draw();
        blueRect.draw();
        greenTriangle.draw();
    }
}
```

#### 案例：消息发送通道

```java
// 实现部分：消息发送器
interface MessageSender {
    void send(String to, String content);
}

class SmsSender implements MessageSender {
    @Override
    public void send(String to, String content) {
        System.out.println("[SMS] 发送到 " + to + ": " + content);
    }
}

class EmailSender implements MessageSender {
    @Override
    public void send(String to, String content) {
        System.out.println("[Email] 发送到 " + to + ": " + content);
    }
}

class WechatSender implements MessageSender {
    @Override
    public void send(String to, String content) {
        System.out.println("[Wechat] 发送到 " + to + ": " + content);
    }
}

// 抽象部分：消息类型
abstract class AbstractMessage {
    protected MessageSender sender;

    public AbstractMessage(MessageSender sender) {
        this.sender = sender;
    }

    abstract void send(String to, String content);
}

class UrgentMessage extends AbstractMessage {
    public UrgentMessage(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String to, String content) {
        sender.send(to, "【紧急】" + content);
    }
}

class NormalMessage extends AbstractMessage {
    public NormalMessage(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String to, String content) {
        sender.send(to, content);
    }
}
```

---

### 2.3 组合模式（Composite）

#### 意图

将对象组合成树形结构以表示"部分-整体"的层次结构，使得用户对单个对象和组合对象的使用具有一致性。

#### 适用场景

- 表示对象的"部分-整体"层次结构（如文件系统、UI 组件树、组织结构）
- 希望用户忽略组合对象与单个对象的不同，统一使用它们

#### 基本结构

```java
// 抽象组件
interface Component {
    void operation();
    void add(Component component);
    void remove(Component component);
    Component getChild(int index);
}

// 叶子节点
class Leaf implements Component {
    private final String name;

    public Leaf(String name) {
        this.name = name;
    }

    @Override
    public void operation() {
        System.out.println("叶子 " + name + " 执行操作");
    }

    @Override
    public void add(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Component component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Component getChild(int index) {
        throw new UnsupportedOperationException();
    }
}

// 组合节点
class Composite implements Component {
    private final String name;
    private final List<Component> children = new ArrayList<>();

    public Composite(String name) {
        this.name = name;
    }

    @Override
    public void operation() {
        System.out.println("组合 " + name + " 执行操作");
        for (Component child : children) {
            child.operation();
        }
    }

    @Override
    public void add(Component component) {
        children.add(component);
    }

    @Override
    public void remove(Component component) {
        children.remove(component);
    }

    @Override
    public Component getChild(int index) {
        return children.get(index);
    }
}
```

#### 案例：文件系统

```java
interface FileSystemNode {
    String getName();
    long getSize();
    void ls(String prefix);
}

class FileNode implements FileSystemNode {
    private final String name;
    private final long size;

    public FileNode(String name, long size) {
        this.name = name;
        this.size = size;
    }

    @Override
    public String getName() { return name; }

    @Override
    public long getSize() { return size; }

    @Override
    public void ls(String prefix) {
        System.out.println(prefix + "├── " + name + " (" + size + " bytes)");
    }
}

class DirectoryNode implements FileSystemNode {
    private final String name;
    private final List<FileSystemNode> children = new ArrayList<>();

    public DirectoryNode(String name) {
        this.name = name;
    }

    public void add(FileSystemNode node) {
        children.add(node);
    }

    @Override
    public String getName() { return name; }

    @Override
    public long getSize() {
        return children.stream()
                .mapToLong(FileSystemNode::getSize)
                .sum();
    }

    @Override
    public void ls(String prefix) {
        System.out.println(prefix + "├── " + name + "/ (" + getSize() + " bytes)");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).ls(prefix + "│   ");
        }
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        DirectoryNode root = new DirectoryNode("root");
        root.add(new FileNode("README.md", 1024));

        DirectoryNode src = new DirectoryNode("src");
        src.add(new FileNode("Main.java", 2048));
        src.add(new FileNode("Utils.java", 512));
        root.add(src);

        root.ls("");
        System.out.println("总大小: " + root.getSize() + " bytes");
    }
}
```

---

### 2.4 装饰器模式（Decorator）

#### 意图

动态地给一个对象添加一些额外的职责，就增加功能来说，装饰器模式比生成子类更为灵活。

#### 适用场景

- 在不影响其他对象的情况下，以动态、透明的方式给单个对象添加职责
- 处理那些可以撤销的职责
- 当不能使用子类进行扩展时（如类被 final 修饰）

#### 基本结构

```java
// 抽象组件
interface Coffee {
    String getDescription();
    double getCost();
}

// 具体组件
class SimpleCoffee implements Coffee {
    @Override
    public String getDescription() {
        return "黑咖啡";
    }

    @Override
    public double getCost() {
        return 10.0;
    }
}

// 抽象装饰器
abstract class CoffeeDecorator implements Coffee {
    protected final Coffee decoratedCoffee;

    public CoffeeDecorator(Coffee coffee) {
        this.decoratedCoffee = coffee;
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription();
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost();
    }
}

// 具体装饰器：加牛奶
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + 牛奶";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 3.0;
    }
}

// 具体装饰器：加糖
class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public String getDescription() {
        return decoratedCoffee.getDescription() + " + 糖";
    }

    @Override
    public double getCost() {
        return decoratedCoffee.getCost() + 1.0;
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " : ¥" + coffee.getCost());

        // 加牛奶
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " : ¥" + coffee.getCost());

        // 再加一份糖
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " : ¥" + coffee.getCost());
    }
}
```

#### 案例：数据流加密处理

```java
interface DataStream {
    byte[] read();
    void write(byte[] data);
}

class FileDataStream implements DataStream {
    private final String filePath;

    public FileDataStream(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public byte[] read() {
        System.out.println("从文件 " + filePath + " 读取数据");
        return ("文件内容").getBytes();
    }

    @Override
    public void write(byte[] data) {
        System.out.println("向文件 " + filePath + " 写入数据");
    }
}

abstract class DataStreamDecorator implements DataStream {
    protected final DataStream stream;

    public DataStreamDecorator(DataStream stream) {
        this.stream = stream;
    }

    @Override
    public byte[] read() {
        return stream.read();
    }

    @Override
    public void write(byte[] data) {
        stream.write(data);
    }
}

class EncryptDecorator extends DataStreamDecorator {
    public EncryptDecorator(DataStream stream) {
        super(stream);
    }

    @Override
    public void write(byte[] data) {
        byte[] encrypted = encrypt(data);
        stream.write(encrypted);
    }

    @Override
    public byte[] read() {
        byte[] data = stream.read();
        return decrypt(data);
    }

    private byte[] encrypt(byte[] data) {
        System.out.println("加密数据...");
        return data; // 模拟加密
    }

    private byte[] decrypt(byte[] data) {
        System.out.println("解密数据...");
        return data; // 模拟解密
    }
}

class CompressDecorator extends DataStreamDecorator {
    public CompressDecorator(DataStream stream) {
        super(stream);
    }

    @Override
    public void write(byte[] data) {
        System.out.println("压缩数据...");
        stream.write(data);
    }

    @Override
    public byte[] read() {
        byte[] data = stream.read();
        System.out.println("解压数据...");
        return data;
    }
}
```

#### JDK 中的装饰器

- `java.io.BufferedInputStream` / `BufferedOutputStream`
- `java.io.DataInputStream` / `DataOutputStream`
- `java.util.Collections.synchronizedXxx()` / `unmodifiableXxx()`

---

### 2.5 外观模式（Facade）

#### 意图

为子系统中的一组接口提供一个统一的高层接口，使子系统更容易使用。

#### 适用场景

- 简化复杂子系统的使用
- 将子系统与客户端解耦，降低客户端与子系统之间的耦合度
- 构建层次结构系统时，使用外观来定义每层的入口

#### 基本结构

```java
// 复杂子系统：多个类
class CPU {
    public void start() {
        System.out.println("CPU 启动");
    }

    public void execute() {
        System.out.println("CPU 执行指令");
    }

    public void shutdown() {
        System.out.println("CPU 关闭");
    }
}

class Memory {
    public void load() {
        System.out.println("内存加载数据");
    }

    public void clear() {
        System.out.println("内存清理");
    }
}

class HardDrive {
    public byte[] read(long sector, int size) {
        System.out.println("硬盘读取: sector=" + sector + ", size=" + size);
        return new byte[size];
    }
}

// 外观类
class ComputerFacade {
    private final CPU cpu = new CPU();
    private final Memory memory = new Memory();
    private final HardDrive hardDrive = new HardDrive();

    public void start() {
        System.out.println("=== 电脑启动 ===");
        cpu.start();
        memory.load();
        hardDrive.read(0, 1024);
        cpu.execute();
        System.out.println("=== 启动完成 ===");
    }

    public void shutdown() {
        System.out.println("=== 电脑关机 ===");
        memory.clear();
        cpu.shutdown();
        System.out.println("=== 关机完成 ===");
    }
}

// 客户端
public class App {
    public static void main(String[] args) {
        ComputerFacade computer = new ComputerFacade();
        computer.start();
        computer.shutdown();
    }
}
```

#### 案例：订单服务外观

```java
// 复杂子系统
class InventoryService {
    public boolean checkStock(String productId, int quantity) {
        System.out.println("检查库存: " + productId + " x " + quantity);
        return true;
    }

    public void reduceStock(String productId, int quantity) {
        System.out.println("减少库存: " + productId + " x " + quantity);
    }
}

class PaymentService {
    public boolean processPayment(String orderId, double amount) {
        System.out.println("处理支付: " + orderId + " ¥" + amount);
        return true;
    }
}

class ShippingService {
    public String scheduleDelivery(String orderId, String address) {
        System.out.println("安排配送: " + orderId + " → " + address);
        return "TKD" + System.currentTimeMillis();
    }
}

class NotificationService {
    public void sendConfirmation(String userId, String orderId) {
        System.out.println("发送确认通知: " + userId + " " + orderId);
    }
}

// 外观
class OrderFacade {
    private final InventoryService inventory = new InventoryService();
    private final PaymentService payment = new PaymentService();
    private final ShippingService shipping = new ShippingService();
    private final NotificationService notification = new NotificationService();

    public String placeOrder(String userId, String productId, int quantity,
                              String address, double amount) {
        // 封装复杂的下单流程
        if (!inventory.checkStock(productId, quantity)) {
            throw new RuntimeException("库存不足");
        }

        String orderId = "ORD" + System.currentTimeMillis();

        if (!payment.processPayment(orderId, amount)) {
            throw new RuntimeException("支付失败");
        }

        inventory.reduceStock(productId, quantity);
        shipping.scheduleDelivery(orderId, address);
        notification.sendConfirmation(userId, orderId);

        return orderId;
    }
}
```

---

### 2.6 享元模式（Flyweight）

#### 意图

运用共享技术有效地支持大量细粒度的对象。

#### 核心概念

享元模式通过共享来减少内存占用。将对象的状态分为**内部状态**（可共享，不随环境变化）和**外部状态**（不可共享，随环境变化）。

#### 适用场景

- 系统中有大量相似对象，造成内存大量消耗
- 对象的大部分状态可以外部化
- 需要缓冲池的场景

#### 基本结构

```java
// 抽象的享元
interface Flyweight {
    void operation(String externalState); // externalState 为外部状态
}

// 具体享元
class ConcreteFlyweight implements Flyweight {
    private final String intrinsicState; // 内部状态：可共享

    public ConcreteFlyweight(String intrinsicState) {
        this.intrinsicState = intrinsicState;
    }

    @Override
    public void operation(String externalState) {
        System.out.println("内部状态: " + intrinsicState + ", 外部状态: " + externalState);
    }
}

// 享元工厂
class FlyweightFactory {
    private final Map<String, Flyweight> pool = new HashMap<>();

    public Flyweight getFlyweight(String key) {
        if (!pool.containsKey(key)) {
            pool.put(key, new ConcreteFlyweight(key));
            System.out.println("创建新享元: " + key);
        }
        return pool.get(key);
    }

    public int getPoolSize() {
        return pool.size();
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        FlyweightFactory factory = new FlyweightFactory();

        Flyweight f1 = factory.getFlyweight("A");
        Flyweight f2 = factory.getFlyweight("A");
        Flyweight f3 = factory.getFlyweight("B");

        System.out.println(f1 == f2); // true，共享同一对象
        System.out.println("池大小: " + factory.getPoolSize()); // 2
    }
}
```

#### 案例：在线游戏中的子弹

```java
// 子弹类型（享元）：可共享的内部状态
class BulletType {
    private final String name;
    private final String texture;
    private final int damage;
    private final double speed;

    public BulletType(String name, String texture, int damage, double speed) {
        this.name = name;
        this.texture = texture;
        this.damage = damage;
        this.speed = speed;
    }

    public String getName() { return name; }
    public String getTexture() { return texture; }
    public int getDamage() { return damage; }
    public double getSpeed() { return speed; }
}

// 子弹实例（非享元）：外部状态
class Bullet {
    private final BulletType type; // 共享的内部状态
    private double x, y;           // 外部状态：位置
    private double angle;          // 外部状态：角度

    public Bullet(BulletType type, double x, double y, double angle) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void move() {
        x += type.getSpeed() * Math.cos(angle);
        y += type.getSpeed() * Math.sin(angle);
    }
}

// 子弹类型工厂
class BulletTypeFactory {
    private static final Map<String, BulletType> CACHE = new HashMap<>();

    public static BulletType getBulletType(String name, String texture,
                                            int damage, double speed) {
        return CACHE.computeIfAbsent(name,
                k -> new BulletType(name, texture, damage, speed));
    }
}
```

#### JDK 中的享元

- `Integer.valueOf(int)`：小整数缓存 [-128, 127]
- `String` 常量池
- 各种连接池：数据库连接池、线程池

---

### 2.7 代理模式（Proxy）

#### 意图

为其他对象提供一种代理以控制对这个对象的访问。

#### 代理的分类

| 类型 | 功能 | 典型场景 |
|------|------|---------|
| 远程代理 | 为远程对象提供本地代表 | RMI、RPC |
| 虚拟代理 | 延迟创建开销大的对象 | 懒加载大图 |
| 保护代理 | 控制对原始对象的访问权限 | 权限校验 |
| 缓存代理 | 为开销大的运算结果提供临时存储 | 查询缓存 |
| 日志代理 | 记录对目标对象的访问 | 审计日志 |

#### 静态代理

```java
interface Subject {
    void request();
}

class RealSubject implements Subject {
    @Override
    public void request() {
        System.out.println("真实对象处理请求");
    }
}

class Proxy implements Subject {
    private final RealSubject realSubject;

    public Proxy(RealSubject realSubject) {
        this.realSubject = realSubject;
    }

    @Override
    public void request() {
        // 前置增强
        System.out.println("代理: 日志记录开始");

        realSubject.request();

        // 后置增强
        System.out.println("代理: 日志记录结束");
    }
}
```

#### 动态代理（JDK）

```java
class LoggingInvocationHandler implements InvocationHandler {
    private final Object target;

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("[LOG] 调用方法: " + method.getName());

        long start = System.currentTimeMillis();
        Object result = method.invoke(target, args);
        long end = System.currentTimeMillis();

        System.out.println("[LOG] 方法耗时: " + (end - start) + "ms");
        return result;
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Subject realSubject = new RealSubject();

        Subject proxy = (Subject) Proxy.newProxyInstance(
                Subject.class.getClassLoader(),
                new Class[]{Subject.class},
                new LoggingInvocationHandler(realSubject)
        );

        proxy.request();
    }
}
```

#### CGLIB 动态代理

```java
class CglibProxy implements MethodInterceptor {

    public Object getProxy(Class<?> clazz) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(this);
        return enhancer.create();
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        System.out.println("[CGLIB] 调用方法: " + method.getName());
        return proxy.invokeSuper(obj, args);
    }
}
```

#### 案例：缓存代理

```java
class CachedUserServiceProxy implements InvocationHandler {

    private final UserService target;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public CachedUserServiceProxy(UserService target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!method.getName().startsWith("get")) {
            return method.invoke(target, args);
        }

        String cacheKey = method.getName() + Arrays.toString(args);
        if (cache.containsKey(cacheKey)) {
            System.out.println("缓存命中: " + cacheKey);
            return cache.get(cacheKey);
        }

        Object result = method.invoke(target, args);
        cache.put(cacheKey, result);
        return result;
    }
}
```

#### Spring AOP 与代理

Spring AOP 默认使用 JDK 动态代理（针对接口），若目标类没有实现接口则使用 CGLIB。`@Transactional`、`@Cacheable`、`@Async` 等注解都基于代理模式实现。

---

## 3. 行为型模式（Behavioral Patterns）

行为型模式关注对象之间的通信，描述对象之间怎样相互协作完成单个对象无法单独完成的任务。

---

### 3.1 责任链模式（Chain of Responsibility）

#### 意图

使多个对象都有机会处理请求，从而避免请求的发送者和接收者之间的耦合关系。将这些对象连成一条链，并沿着这条链传递请求，直到有一个对象处理它为止。

#### 适用场景

- 有多个对象可以处理一个请求，哪个对象处理该请求由运行时刻自动确定
- 在不明确指定接收者的情况下，向多个对象中的一个提交请求
- 可处理一个请求的对象集合需要被动态指定

#### 基本结构

```java
// 抽象处理者
abstract class Handler {
    protected Handler next;

    public void setNext(Handler next) {
        this.next = next;
    }

    public abstract void handleRequest(Request request);
}

class Request {
    private final String type;
    private final String content;

    public Request(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public String getType() { return type; }
    public String getContent() { return content; }
}

// 具体处理者
class AuthenticationHandler extends Handler {
    @Override
    public void handleRequest(Request request) {
        if ("auth".equals(request.getType())) {
            System.out.println("认证处理器: 处理 " + request.getContent());
            return;
        }
        if (next != null) {
            next.handleRequest(request);
        }
    }
}

class AuthorizationHandler extends Handler {
    @Override
    public void handleRequest(Request request) {
        if ("permission".equals(request.getType())) {
            System.out.println("授权处理器: 处理 " + request.getContent());
            return;
        }
        if (next != null) {
            next.handleRequest(request);
        }
    }
}

class LogHandler extends Handler {
    @Override
    public void handleRequest(Request request) {
        System.out.println("日志处理器: 记录 " + request.getType() + " - " + request.getContent());
        if (next != null) {
            next.handleRequest(request);
        }
    }
}
```

#### 案例：Web 过滤器链

```java
interface Filter {
    void doFilter(Request request, Response response, FilterChain chain);
}

class FilterChain {
    private final List<Filter> filters = new ArrayList<>();
    private int index = 0;

    public FilterChain addFilter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public void doFilter(Request request, Response response) {
        if (index < filters.size()) {
            Filter filter = filters.get(index++);
            filter.doFilter(request, response, this);
        }
    }
}

class IpBlacklistFilter implements Filter {
    private final Set<String> blacklist = Set.of("192.168.1.100");

    @Override
    public void doFilter(Request request, Response response, FilterChain chain) {
        if (blacklist.contains(request.getClientIp())) {
            response.setStatus(403);
            response.setBody("IP 被封禁");
            return;
        }
        chain.doFilter(request, response);
    }
}

class RateLimitFilter implements Filter {
    @Override
    public void doFilter(Request request, Response response, FilterChain chain) {
        if (isRateLimited(request.getClientIp())) {
            response.setStatus(429);
            response.setBody("请求过于频繁");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRateLimited(String ip) {
        return false; // 模拟
    }
}

class XssFilter implements Filter {
    @Override
    public void doFilter(Request request, Response response, FilterChain chain) {
        request.setBody(sanitize(request.getBody()));
        chain.doFilter(request, response);
    }

    private String sanitize(String input) {
        return input.replace("<script>", "&lt;script&gt;");
    }
}
```

#### Servlet Filter 就是典型的责任链模式

---

### 3.2 命令模式（Command）

#### 意图

将一个请求封装为一个对象，从而使你可用不同的请求对客户进行参数化；对请求排队或记录请求日志，以及支持可撤销的操作。

#### 适用场景

- 需要将请求调用者和请求接收者解耦
- 需要抽象出待执行的动作以参数化某对象
- 支持撤销/重做操作
- 支持命令队列、宏命令、日志记录

#### 基本结构

```java
// 命令接口
interface Command {
    void execute();
    void undo();
}

// 接收者
class Light {
    public void turnOn() {
        System.out.println("灯开了");
    }

    public void turnOff() {
        System.out.println("灯关了");
    }
}

// 具体命令
class LightOnCommand implements Command {
    private final Light light;

    public LightOnCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.turnOn();
    }

    @Override
    public void undo() {
        light.turnOff();
    }
}

class LightOffCommand implements Command {
    private final Light light;

    public LightOffCommand(Light light) {
        this.light = light;
    }

    @Override
    public void execute() {
        light.turnOff();
    }

    @Override
    public void undo() {
        light.turnOn();
    }
}

// 调用者
class RemoteControl {
    private final List<Command> history = new ArrayList<>();

    public void press(Command command) {
        command.execute();
        history.add(command);
    }

    public void undoLast() {
        if (!history.isEmpty()) {
            Command last = history.remove(history.size() - 1);
            last.undo();
        }
    }
}
```

#### 案例：文本编辑器操作

```java
interface EditorCommand {
    void execute();
    void undo();
}

class TextEditor {
    private final StringBuilder content = new StringBuilder();
    private int cursorPosition = 0;

    public void insert(int position, String text) {
        content.insert(position, text);
        cursorPosition = position + text.length();
    }

    public String delete(int position, int length) {
        String deleted = content.substring(position, position + length);
        content.delete(position, position + length);
        cursorPosition = position;
        return deleted;
    }

    public String getText() {
        return content.toString();
    }
}

class InsertCommand implements EditorCommand {
    private final TextEditor editor;
    private final int position;
    private final String text;

    public InsertCommand(TextEditor editor, int position, String text) {
        this.editor = editor;
        this.position = position;
        this.text = text;
    }

    @Override
    public void execute() {
        editor.insert(position, text);
    }

    @Override
    public void undo() {
        editor.delete(position, text.length());
    }
}

class DeleteCommand implements EditorCommand {
    private final TextEditor editor;
    private final int position;
    private final int length;
    private String deletedText;

    public DeleteCommand(TextEditor editor, int position, int length) {
        this.editor = editor;
        this.position = position;
        this.length = length;
    }

    @Override
    public void execute() {
        deletedText = editor.delete(position, length);
    }

    @Override
    public void undo() {
        editor.insert(position, deletedText);
    }
}

// 命令管理器
class CommandHistory {
    private final Stack<EditorCommand> undoStack = new Stack<>();
    private final Stack<EditorCommand> redoStack = new Stack<>();

    public void execute(EditorCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorCommand cmd = redoStack.pop();
            cmd.execute();
            undoStack.push(cmd);
        }
    }
}
```

#### JDK 中的命令模式

- `java.lang.Runnable` 本身就是命令模式的体现
- `javax.swing.Action`

---

### 3.3 解释器模式（Interpreter）

#### 意图

给定一个语言，定义它的文法的一种表示，并定义一个解释器，这个解释器使用该表示来解释语言中的句子。

#### 适用场景

- 一种特定类型的问题发生的频率足够高，需要将该问题的各个实例表述为一个简单语言中的句子
- 构建一个简单语言的语法解释器，如正则表达式、SQL 解析、数学表达式求值

#### 案例：四则运算表达式求值

```java
// 抽象表达式
interface Expression {
    int interpret(Map<String, Integer> context);
}

// 终结符表达式：变量
class VariableExpression implements Expression {
    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public int interpret(Map<String, Integer> context) {
        return context.getOrDefault(name, 0);
    }
}

// 终结符表达式：数字
class NumberExpression implements Expression {
    private final int value;

    public NumberExpression(int value) {
        this.value = value;
    }

    @Override
    public int interpret(Map<String, Integer> context) {
        return value;
    }
}

// 非终结符表达式：加法
class AddExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public AddExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int interpret(Map<String, Integer> context) {
        return left.interpret(context) + right.interpret(context);
    }
}

// 非终结符表达式：减法
class SubtractExpression implements Expression {
    private final Expression left;
    private final Expression right;

    public SubtractExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int interpret(Map<String, Integer> context) {
        return left.interpret(context) - right.interpret(context);
    }
}

// 解析器
class ExpressionParser {
    public static Expression parse(String expr) {
        Stack<Expression> stack = new Stack<>();
        String[] tokens = expr.split(" ");

        for (String token : tokens) {
            if (token.equals("+")) {
                Expression right = stack.pop();
                Expression left = stack.pop();
                stack.push(new AddExpression(left, right));
            } else if (token.equals("-")) {
                Expression right = stack.pop();
                Expression left = stack.pop();
                stack.push(new SubtractExpression(left, right));
            } else {
                try {
                    stack.push(new NumberExpression(Integer.parseInt(token)));
                } catch (NumberFormatException e) {
                    stack.push(new VariableExpression(token));
                }
            }
        }
        return stack.pop();
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        String expr = "x y + 5 -";
        Expression expression = ExpressionParser.parse(expr);

        Map<String, Integer> context = Map.of("x", 10, "y", 3);
        int result = expression.interpret(context);

        System.out.println(expr + " = " + result); // (10 + 3) - 5 = 8
    }
}
```

---

### 3.4 迭代器模式（Iterator）

#### 意图

提供一种方法顺序访问一个聚合对象中各个元素，而又不需暴露该对象的内部表示。

#### 适用场景

- 访问一个聚合对象的内容而无需暴露它的内部表示
- 支持对聚合对象的多种遍历
- 为不同的聚合结构提供一个统一的接口

#### JDK 中的迭代器

```java
// Java 内置迭代器使用
List<String> list = new ArrayList<>();
list.add("A");
list.add("B");
list.add("C");

Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    System.out.println(item);
}
```

#### 案例：自定义集合的迭代器

```java
class TreeNode<T> {
    T value;
    TreeNode<T> left;
    TreeNode<T> right;

    public TreeNode(T value) {
        this.value = value;
    }
}

// 二叉树中序遍历迭代器
class TreeIterator<T> implements Iterator<T> {
    private final Stack<TreeNode<T>> stack = new Stack<>();

    public TreeIterator(TreeNode<T> root) {
        pushLeft(root);
    }

    private void pushLeft(TreeNode<T> node) {
        while (node != null) {
            stack.push(node);
            node = node.left;
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public T next() {
        TreeNode<T> node = stack.pop();
        pushLeft(node.right);
        return node.value;
    }
}

// 包装为 Iterable
class BinaryTree<T> implements Iterable<T> {
    private final TreeNode<T> root;

    public BinaryTree(TreeNode<T> root) {
        this.root = root;
    }

    @Override
    public Iterator<T> iterator() {
        return new TreeIterator<>(root);
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        TreeNode<Integer> root = new TreeNode<>(5);
        root.left = new TreeNode<>(3);
        root.right = new TreeNode<>(7);
        root.left.left = new TreeNode<>(1);
        root.left.right = new TreeNode<>(4);

        BinaryTree<Integer> tree = new BinaryTree<>(root);
        for (int value : tree) {
            System.out.print(value + " "); // 1 3 4 5 7
        }
    }
}
```

---

### 3.5 中介者模式（Mediator）

#### 意图

用一个中介对象来封装一系列对象之间的交互。中介者使各对象不需要显式地相互引用，从而使其耦合松散，而且可以独立地改变它们之间的交互。

#### 适用场景

- 一组对象以定义良好但复杂的方式进行通信，产生的相互依赖关系结构混乱且难以理解
- 一个对象引用其他很多对象并且直接与这些对象通信，导致难以复用该对象
- 想通过一个中间类来封装多个类中的行为，而又不想生成太多的子类

#### 案例：聊天室

```java
// 中介者
interface ChatMediator {
    void sendMessage(String message, User sender, String receiverName);
    void addUser(User user);
}

class ChatRoom implements ChatMediator {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public void addUser(User user) {
        users.put(user.getName(), user);
        System.out.println(user.getName() + " 加入聊天室");
    }

    @Override
    public void sendMessage(String message, User sender, String receiverName) {
        User receiver = users.get(receiverName);
        if (receiver != null) {
            receiver.receive(message, sender.getName());
        } else {
            System.out.println("用户 " + receiverName + " 不在线");
        }
    }
}

// 同事类
abstract class User {
    protected ChatMediator mediator;
    protected String name;

    public User(ChatMediator mediator, String name) {
        this.mediator = mediator;
        this.name = name;
    }

    public String getName() { return name; }

    public abstract void send(String message, String to);
    public abstract void receive(String message, String from);
}

class ChatUser extends User {
    public ChatUser(ChatMediator mediator, String name) {
        super(mediator, name);
    }

    @Override
    public void send(String message, String to) {
        System.out.println(name + " 发送消息给 " + to + ": " + message);
        mediator.sendMessage(message, this, to);
    }

    @Override
    public void receive(String message, String from) {
        System.out.println(name + " 收到来自 " + from + " 的消息: " + message);
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        ChatRoom room = new ChatRoom();

        ChatUser alice = new ChatUser(room, "Alice");
        ChatUser bob = new ChatUser(room, "Bob");
        ChatUser charlie = new ChatUser(room, "Charlie");

        room.addUser(alice);
        room.addUser(bob);
        room.addUser(charlie);

        alice.send("你好 Bob!", "Bob");
        bob.send("你好 Alice!", "Alice");
    }
}
```

#### 案例：微服务中的事件总线

```java
// 事件总线充当多个微服务之间的中介者
class EventBus {
    private final Map<String, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void publish(String eventType, Object event) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }
}

interface EventListener {
    void onEvent(Object event);
}
```

---

### 3.6 备忘录模式（Memento）

#### 意图

在不破坏封装性的前提下，捕获一个对象的内部状态，并在该对象之外保存这个状态。这样以后就可将该对象恢复到原先保存的状态。

#### 适用场景

- 需要保存对象在某一时刻的状态，以便之后恢复
- 直接获取对象的状态会暴露对象的实现细节、破坏封装性

#### 案例：游戏存档

```java
// 备忘录
class GameMemento {
    private final int level;
    private final int score;
    private final int health;
    private final int mana;

    public GameMemento(int level, int score, int health, int mana) {
        this.level = level;
        this.score = score;
        this.health = health;
        this.mana = mana;
    }

    public int getLevel() { return level; }
    public int getScore() { return score; }
    public int getHealth() { return health; }
    public int getMana() { return mana; }
}

// 发起人
class Game {
    private int level = 1;
    private int score = 0;
    private int health = 100;
    private int mana = 100;

    public void play() {
        level++;
        score += 1000;
        health -= 20;
        mana -= 10;
    }

    public void showStatus() {
        System.out.println("Level: " + level + ", Score: " + score
                + ", Health: " + health + ", Mana: " + mana);
    }

    public GameMemento save() {
        return new GameMemento(level, score, health, mana);
    }

    public void restore(GameMemento memento) {
        this.level = memento.getLevel();
        this.score = memento.getScore();
        this.health = memento.getHealth();
        this.mana = memento.getMana();
    }
}

// 负责人
class SaveManager {
    private final Stack<GameMemento> saves = new Stack<>();

    public void save(GameMemento memento) {
        saves.push(memento);
        System.out.println("游戏已保存");
    }

    public GameMemento load() {
        if (!saves.isEmpty()) {
            System.out.println("游戏已加载");
            return saves.pop();
        }
        return null;
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Game game = new Game();
        SaveManager saveManager = new SaveManager();

        game.showStatus();
        saveManager.save(game.save());

        game.play();
        game.showStatus();

        GameMemento saved = saveManager.load();
        if (saved != null) {
            game.restore(saved);
            game.showStatus();
        }
    }
}
```

---

### 3.7 观察者模式（Observer）

#### 意图

定义对象间的一种一对多的依赖关系，当一个对象的状态发生改变时，所有依赖于它的对象都得到通知并被自动更新。

#### 适用场景

- 一个抽象模型有两个方面，其中一个方面依赖于另一个方面，将它们封装在独立的对象中以使它们可以各自独立地改变和复用
- 一个对象的改变将导致一个或多个其他对象也发生改变，而不知道具体有多少个对象需要改变
- 触发机制

#### 基本结构

```java
// 观察者
interface Observer {
    void update(String message);
}

// 主题
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers(String message);
}

class NewsAgency implements Subject {
    private final List<Observer> observers = new ArrayList<>();

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    public void publishNews(String news) {
        System.out.println("新闻社发布: " + news);
        notifyObservers(news);
    }
}

class NewsSubscriber implements Observer {
    private final String name;

    public NewsSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void update(String message) {
        System.out.println("  [" + name + "] 收到新闻: " + message);
    }
}
```

#### 案例：Spring 事件监听

```java
// 事件
public class OrderCreatedEvent extends ApplicationEvent {
    private final String orderId;
    private final double amount;

    public OrderCreatedEvent(Object source, String orderId, double amount) {
        super(source);
        this.orderId = orderId;
        this.amount = amount;
    }

    public String getOrderId() { return orderId; }
    public double getAmount() { return amount; }
}

// 监听器
@Component
public class OrderNotificationListener
        implements ApplicationListener<OrderCreatedEvent> {

    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        System.out.println("发送订单通知: " + event.getOrderId() + " ¥" + event.getAmount());
    }
}

@Component
public class OrderStatisticsListener
        implements ApplicationListener<OrderCreatedEvent> {

    @Override
    public void onApplicationEvent(OrderCreatedEvent event) {
        System.out.println("更新订单统计: " + event.getOrderId());
    }
}

// 发布事件
@Service
public class OrderService {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void createOrder(String orderId, double amount) {
        // 创建订单逻辑...

        // 发布事件
        publisher.publishEvent(new OrderCreatedEvent(this, orderId, amount));
    }
}
```

#### JDK 中的观察者

- `java.util.Observer` / `java.util.Observable`（Java 9 已废弃）
- `java.beans.PropertyChangeListener`
- Spring 事件机制 (`ApplicationEvent` / `ApplicationListener`)
- RxJava / Project Reactor（响应式编程）

---

### 3.8 状态模式（State）

#### 意图

允许一个对象在其内部状态改变时改变它的行为。对象看起来似乎修改了它的类。

#### 适用场景

- 一个对象的行为取决于它的状态，并且必须在运行时根据状态改变其行为
- 操作中含有庞大的多分支条件语句（if-else / switch），这些分支依赖于对象的状态

#### 基本结构

```java
// 状态接口
interface State {
    void handle(Context context);
}

// 具体状态
class StartState implements State {
    @Override
    public void handle(Context context) {
        System.out.println("当前状态: 启动");
        context.setState(new RunningState());
    }
}

class RunningState implements State {
    @Override
    public void handle(Context context) {
        System.out.println("当前状态: 运行中");
        context.setState(new StopState());
    }
}

class StopState implements State {
    @Override
    public void handle(Context context) {
        System.out.println("当前状态: 停止");
        context.setState(new StartState());
    }
}

// 上下文
class Context {
    private State state;

    public Context() {
        this.state = new StartState();
    }

    public void setState(State state) {
        this.state = state;
    }

    public void request() {
        state.handle(this);
    }
}

// 使用
Context context = new Context();
context.request(); // 当前状态: 启动
context.request(); // 当前状态: 运行中
context.request(); // 当前状态: 停止
```

#### 案例：订单状态机

```java
interface OrderState {
    void pay(Order order);
    void ship(Order order);
    void confirm(Order order);
    void cancel(Order order);
}

class PendingState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("订单支付成功");
        order.setState(new PaidState());
    }

    @Override
    public void ship(Order order) {
        throw new IllegalStateException("待支付订单不能发货");
    }

    @Override
    public void confirm(Order order) {
        throw new IllegalStateException("待支付订单不能确认收货");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("订单已取消");
        order.setState(new CancelledState());
    }
}

class PaidState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("订单已支付，无需重复支付");
    }

    @Override
    public void ship(Order order) {
        System.out.println("订单已发货");
        order.setState(new ShippedState());
    }

    @Override
    public void confirm(Order order) {
        System.out.println("订单未发货，不能确认");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("已支付订单退款中...");
        order.setState(new CancelledState());
    }
}

class ShippedState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("订单已支付");
    }

    @Override
    public void ship(Order order) {
        System.out.println("订单已发货");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("订单确认收货");
        order.setState(new CompletedState());
    }

    @Override
    public void cancel(Order order) {
        throw new IllegalStateException("已发货订单不能取消");
    }
}

class CompletedState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("订单已完成");
    }

    @Override
    public void ship(Order order) {
        System.out.println("订单已完成");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("订单已完成");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("已完成订单不能取消");
    }
}

class CancelledState implements OrderState {
    @Override
    public void pay(Order order) {
        System.out.println("已取消订单不能支付");
    }

    @Override
    public void ship(Order order) {
        System.out.println("已取消订单不能发货");
    }

    @Override
    public void confirm(Order order) {
        System.out.println("已取消订单不能确认");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("订单已取消");
    }
}

class Order {
    private OrderState state = new PendingState();

    public void setState(OrderState state) {
        this.state = state;
    }

    public void pay() { state.pay(this); }
    public void ship() { state.ship(this); }
    public void confirm() { state.confirm(this); }
    public void cancel() { state.cancel(this); }
}
```

#### 状态模式 vs 策略模式

| 维度 | 状态模式 | 策略模式 |
|------|---------|---------|
| 关注点 | 对象内部状态变化导致行为变化 | 算法的互换 |
| 状态变化 | 状态自动切换（内部闭环） | 策略由客户端指定（外部切换） |
| 上下文耦合 | 状态间相互持有引用，耦合较强 | 策略间无引用，相互独立 |

---

### 3.9 策略模式（Strategy）

#### 意图

定义一系列的算法，把它们一个个封装起来，并且使它们可相互替换。策略模式使算法可独立于使用它的客户而变化。

#### 适用场景

- 需要在运行时选择算法的不同变体
- 许多相关的类仅仅是行为有异
- 需要避免暴露复杂的、与算法相关的数据结构
- 一个类定义了多种行为，这些行为以多个条件语句的形式出现

#### 基本结构

```java
// 策略接口
interface SortStrategy {
    void sort(int[] array);
}

class BubbleSort implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("冒泡排序");
        // 冒泡排序实现
    }
}

class QuickSort implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("快速排序");
        // 快速排序实现
    }
}

class MergeSort implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("归并排序");
        // 归并排序实现
    }
}

// 上下文
class Sorter {
    private SortStrategy strategy;

    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }

    public void sort(int[] array) {
        strategy.sort(array);
    }
}

// 使用
public class App {
    public static void main(String[] args) {
        Sorter sorter = new Sorter();
        int[] data = {5, 2, 8, 1, 9};

        // 小数据量用冒泡
        sorter.setStrategy(new BubbleSort());
        sorter.sort(data);

        // 大数据量用快速排序
        sorter.setStrategy(new QuickSort());
        sorter.sort(data);
    }
}
```

#### 案例：支付策略

```java
interface PaymentStrategy {
    boolean pay(double amount);
}

class AlipayStrategy implements PaymentStrategy {
    @Override
    public boolean pay(double amount) {
        System.out.println("支付宝支付: ¥" + amount);
        return true;
    }
}

class WechatPayStrategy implements PaymentStrategy {
    @Override
    public boolean pay(double amount) {
        System.out.println("微信支付: ¥" + amount);
        return true;
    }
}

class UnionPayStrategy implements PaymentStrategy {
    @Override
    public boolean pay(double amount) {
        System.out.println("银联支付: ¥" + amount);
        return true;
    }
}

// 支付上下文
class PaymentContext {
    private PaymentStrategy strategy;

    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean executePayment(double amount) {
        return strategy.pay(amount);
    }
}

// 策略工厂（配合使用）
class PaymentStrategyFactory {
    public static PaymentStrategy create(String type) {
        return switch (type) {
            case "alipay" -> new AlipayStrategy();
            case "wechat" -> new WechatPayStrategy();
            case "union" -> new UnionPayStrategy();
            default -> throw new IllegalArgumentException("不支持的支付方式: " + type);
        };
    }
}

// 使用
PaymentContext context = new PaymentContext(
        PaymentStrategyFactory.create("alipay")
);
context.executePayment(99.99);
```

#### 案例：参数校验策略

```java
interface ValidationStrategy {
    List<String> validate(Object value);
}

class NotNullValidation implements ValidationStrategy {
    private final String fieldName;

    public NotNullValidation(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public List<String> validate(Object value) {
        if (value == null) {
            return List.of(fieldName + " 不能为空");
        }
        return List.of();
    }
}

class RangeValidation implements ValidationStrategy {
    private final String fieldName;
    private final int min;
    private final int max;

    public RangeValidation(String fieldName, int min, int max) {
        this.fieldName = fieldName;
        this.min = min;
        this.max = max;
    }

    @Override
    public List<String> validate(Object value) {
        int intValue = (int) value;
        if (intValue < min || intValue > max) {
            return List.of(fieldName + " 必须在 " + min + " ~ " + max + " 之间");
        }
        return List.of();
    }
}

class RegexValidation implements ValidationStrategy {
    private final String fieldName;
    private final String pattern;

    public RegexValidation(String fieldName, String pattern) {
        this.fieldName = fieldName;
        this.pattern = pattern;
    }

    @Override
    public List<String> validate(Object value) {
        String strValue = (String) value;
        if (!strValue.matches(pattern)) {
            return List.of(fieldName + " 格式不正确");
        }
        return List.of();
    }
}

// 校验器
class Validator {
    private final Map<String, List<ValidationStrategy>> strategies = new HashMap<>();

    public void addRule(String field, ValidationStrategy strategy) {
        strategies.computeIfAbsent(field, k -> new ArrayList<>()).add(strategy);
    }

    public List<String> validate(Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, List<ValidationStrategy>> entry : strategies.entrySet()) {
            String field = entry.getKey();
            Object value = data.get(field);
            for (ValidationStrategy strategy : entry.getValue()) {
                errors.addAll(strategy.validate(value));
            }
        }
        return errors;
    }
}

// 使用
Validator validator = new Validator();
validator.addRule("name", new NotNullValidation("姓名"));
validator.addRule("age", new RangeValidation("年龄", 1, 150));
validator.addRule("email", new RegexValidation("邮箱", "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"));

Map<String, Object> data = Map.of("name", "张三", "age", 200, "email", "invalid");
List<String> errors = validator.validate(data);
// 输出: ["年龄 必须在 1 ~ 150 之间", "邮箱 格式不正确"]
```

#### JDK 中的策略模式

- `java.util.Comparator` 是最典型的策略模式
- `java.util.concurrent.ThreadPoolExecutor` 的 `RejectedExecutionHandler` 四种拒绝策略
- Spring 中 `Resource` 接口的不同实现

---

### 3.10 模板方法模式（Template Method）

#### 意图

定义一个操作中算法的骨架，而将一些步骤延迟到子类中。模板方法使得子类可以不改变一个算法的结构即可重定义该算法的某些特定步骤。

#### 适用场景

- 一次性实现一个算法的不变部分，并将可变的行为留给子类来实现
- 各子类中的公共行为应被提取出来并集中到一个公共父类中，以避免代码重复
- 控制子类扩展（钩子方法）

#### 基本结构

```java
abstract class DataProcessor {

    // 模板方法（final 防止子类重写）
    public final void process() {
        readData();
        if (validate()) {
            transform();
            writeData();
        } else {
            handleInvalidData();
        }
    }

    abstract void readData();
    abstract void transform();
    abstract void writeData();

    // 钩子方法：子类可选择重写
    boolean validate() {
        return true;
    }

    void handleInvalidData() {
        System.out.println("数据校验失败，跳过处理");
    }
}

class CSVProcessor extends DataProcessor {
    @Override
    void readData() {
        System.out.println("读取 CSV 文件");
    }

    @Override
    void transform() {
        System.out.println("转换 CSV 格式");
    }

    @Override
    void writeData() {
        System.out.println("写入转换后的 CSV 数据");
    }

    @Override
    boolean validate() {
        System.out.println("校验 CSV 格式...");
        return true;
    }
}

class JSONProcessor extends DataProcessor {
    @Override
    void readData() {
        System.out.println("读取 JSON 文件");
    }

    @Override
    void transform() {
        System.out.println("转换 JSON 格式");
    }

    @Override
    void writeData() {
        System.out.println("写入转换后的 JSON 数据");
    }
}
```

#### 案例：JdbcTemplate 模拟

```java
abstract class JdbcTemplate {

    public final <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            rs = stmt.executeQuery();

            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rowMapper.mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("查询失败", e);
        } finally {
            closeResultSet(rs);
            closeStatement(stmt);
            closeConnection(conn);
        }
    }

    public final int update(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新失败", e);
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }

    // 子类需实现的抽象方法
    protected abstract Connection getConnection();

    // 钩子：子类可覆盖
    protected void setParameters(PreparedStatement stmt, Object... params)
            throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    protected void closeConnection(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    protected void closeStatement(Statement stmt) {
        if (stmt != null) {
            try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException ignored) {}
        }
    }
}

interface RowMapper<T> {
    T mapRow(ResultSet rs) throws SQLException;
}

// 具体实现
class MySQLJdbcTemplate extends JdbcTemplate {
    @Override
    protected Connection getConnection() {
        // 返回 MySQL 连接
        return null;
    }
}
```

#### JDK / 框架中的模板方法

- `java.io.InputStream.read(byte[], int, int)` 依赖子类实现 `read()`
- `javax.servlet.http.HttpServlet` 的 `doGet()` / `doPost()`
- Spring 的 `JdbcTemplate`、`RestTemplate`、`TransactionTemplate`

---

### 3.11 访问者模式（Visitor）

#### 意图

表示一个作用于某对象结构中的各元素的操作。它使你可以在不改变各元素类的前提下定义作用于这些元素的新操作。

#### 适用场景

- 一个对象结构包含很多类对象，它们有不同的接口，而你想对这些对象实施一些依赖于其具体类的操作
- 需要对一个对象结构中的对象进行很多不同的并且不相关的操作，而你想避免让这些操作"污染"这些对象的类
- 定义对象结构的类很少改变，但经常需要在此结构上定义新的操作

#### 基本结构

```java
// 元素接口
interface Element {
    void accept(Visitor visitor);
}

// 具体元素
class ConcreteElementA implements Element {
    private final String name;

    public ConcreteElementA(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String operationA() {
        return "ElementA 特有操作: " + name;
    }
}

class ConcreteElementB implements Element {
    private final int value;

    public ConcreteElementB(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String operationB() {
        return "ElementB 特有操作: " + value;
    }
}

// 访问者接口
interface Visitor {
    void visit(ConcreteElementA element);
    void visit(ConcreteElementB element);
}

// 具体访问者
class ExportVisitor implements Visitor {
    @Override
    public void visit(ConcreteElementA element) {
        System.out.println("导出 ElementA: " + element.operationA());
    }

    @Override
    public void visit(ConcreteElementB element) {
        System.out.println("导出 ElementB: " + element.operationB());
    }
}

class StatisticsVisitor implements Visitor {
    private int elementACount = 0;
    private int totalValue = 0;

    @Override
    public void visit(ConcreteElementA element) {
        elementACount++;
    }

    @Override
    public void visit(ConcreteElementB element) {
        totalValue += element.getValue();
    }

    public void report() {
        System.out.println("ElementA 数量: " + elementACount);
        System.out.println("ElementB 总价值: " + totalValue);
    }
}

// 对象结构
class ObjectStructure {
    private final List<Element> elements = new ArrayList<>();

    public void add(Element element) {
        elements.add(element);
    }

    public void accept(Visitor visitor) {
        for (Element element : elements) {
            element.accept(visitor);
        }
    }
}

// 使用
ObjectStructure structure = new ObjectStructure();
structure.add(new ConcreteElementA("Foo"));
structure.add(new ConcreteElementA("Bar"));
structure.add(new ConcreteElementB(100));
structure.add(new ConcreteElementB(200));

ExportVisitor exportVisitor = new ExportVisitor();
structure.accept(exportVisitor);

StatisticsVisitor statsVisitor = new StatisticsVisitor();
structure.accept(statsVisitor);
statsVisitor.report();
```

#### 案例：AST（抽象语法树）遍历

```java
interface ASTNode {
    void accept(ASTVisitor visitor);
}

class VariableNode implements ASTNode {
    private final String name;

    public VariableNode(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

class AssignmentNode implements ASTNode {
    private final String variableName;
    private final ASTNode value;

    public AssignmentNode(String variableName, ASTNode value) {
        this.variableName = variableName;
        this.value = value;
    }

    public String getVariableName() { return variableName; }
    public ASTNode getValue() { return value; }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

class NumberLiteralNode implements ASTNode {
    private final int value;

    public NumberLiteralNode(int value) {
        this.value = value;
    }

    public int getValue() { return value; }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}

// 访问者接口
interface ASTVisitor {
    void visit(VariableNode node);
    void visit(AssignmentNode node);
    void visit(NumberLiteralNode node);
}

// TypeCheck：类型检查访问者
class TypeCheckVisitor implements ASTVisitor {
    private final Map<String, String> typeMap = new HashMap<>();

    @Override
    public void visit(VariableNode node) {
        if (!typeMap.containsKey(node.getName())) {
            System.err.println("错误: 变量 " + node.getName() + " 未定义");
        }
    }

    @Override
    public void visit(AssignmentNode node) {
        node.getValue().accept(this);
        typeMap.put(node.getVariableName(), "int");
    }

    @Override
    public void visit(NumberLiteralNode node) {
        // 字面量总是有效的
    }
}

// CodeGen：代码生成访问者
class CodeGenVisitor implements ASTVisitor {
    private final StringBuilder code = new StringBuilder();

    @Override
    public void visit(VariableNode node) {
        code.append("LOAD ").append(node.getName()).append("\n");
    }

    @Override
    public void visit(AssignmentNode node) {
        code.append("PUSH ").append(((NumberLiteralNode) node.getValue()).getValue()).append("\n");
        code.append("STORE ").append(node.getVariableName()).append("\n");
    }

    @Override
    public void visit(NumberLiteralNode node) {
        code.append("PUSH ").append(node.getValue()).append("\n");
    }

    public String getCode() {
        return code.toString();
    }
}
```

#### 访问者模式优缺点

| 优点 | 缺点 |
|------|------|
| 新增操作容易（新增访问者即可） | 新增元素类型困难（所有访问者都要修改） |
| 相关行为集中到一个访问者中 | 破坏了封装（访问者需要了解元素的内部结构） |
| 跨类层级访问 | 元素需要暴露足够多的接口给访问者 |

---

## 4. 设计模式选用原则

### 4.1 六大设计原则

| 原则 | 英文 | 说明 |
|------|------|------|
| 单一职责原则 | Single Responsibility Principle (SRP) | 一个类只负责一项职责 |
| 开闭原则 | Open/Closed Principle (OCP) | 对扩展开放，对修改关闭 |
| 里氏替换原则 | Liskov Substitution Principle (LSP) | 子类可以替换父类而不影响程序正确性 |
| 接口隔离原则 | Interface Segregation Principle (ISP) | 使用多个专门的接口，而非单一总接口 |
| 依赖倒置原则 | Dependency Inversion Principle (DIP) | 依赖抽象而非具体实现 |
| 迪米特法则 | Law of Demeter (LoD) | 一个对象应对其他对象有最少了解 |

### 4.2 设计模式选择指南

#### 按目标分类速查

| 目标 | 推荐模式 |
|------|---------|
| 统一创建对象 | 工厂方法、抽象工厂 |
| 控制对象数量 | 单例、享元 |
| 动态添加功能 | 装饰器、代理 |
| 简化接口 | 外观、适配器 |
| 解耦发送者和接收者 | 命令、观察者、责任链 |
| 处理状态变化 | 状态、策略 |
| 算法骨架不变 | 模板方法 |
| 操作复杂对象结构 | 访问者、组合、迭代器 |

#### 常见反模式

| 反模式 | 改进方向 |
|--------|---------|
| 过度设计（一个简单 CRUD 用了 5 个模式） | 先写最简单的实现，需要时再重构引入 |
| 上帝对象（一个类干了所有事） | 拆分为单一职责的类，使用组合、外观 |
| 单例滥用（全局变量换个名字） | 考虑依赖注入替代 |
| 继承滥用 | 优先使用组合（组合优于继承） |

### 4.3 模式之间的关系

```plaintext
创建型                         结构型                        行为型
======                        ======                       ======
                                                         模板方法 ─── 策略
                                                            │            │
单例 ─── 享元                  装饰器 ─── 代理               ▼            ▼
                                                       状态        命令 ─── 备忘录
工厂方法 ─── 抽象工厂           适配器 ─── 外观                │
  │                                       │               ▼
  ▼                                       ▼            观察者 ─── 中介者
建造者   原型                    组合        桥接               │
                                                            ▼
                                                         责任链   访问者 ─── 迭代器
                                                                    │
                                                                    ▼
                                                                 解释器
```

### 4.4 实践建议

1. **理解意图优先于记忆结构**：知道一个模式解决什么问题，比背下 UML 图更重要。
2. **重构时引入模式**：不要在编码之初就强行套用模式，而是在发现代码恶臭时用模式改善。
3. **模式是交流语言**：团队内对模式有共同理解，可以显著降低沟通成本。
4. **KISS 原则**：Keep It Simple, Stupid。能用简单代码解决问题就不引入模式。
5. **模式不是教条**：可以根据实际情况变形、组合使用。

---

> **参考资料**
>
> - 《Design Patterns: Elements of Reusable Object-Oriented Software》GoF
> - 《Head First Design Patterns》
> - Java API 源码（Collections、IO、JDBC、Servlet）
> - Spring Framework 源码
