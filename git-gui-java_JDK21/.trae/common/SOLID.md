# SOLID 设计原则

> **git-gui 项目适配注**：本文以 Spring 框架为例演示 SOLID 原则的落地实践。git-gui 项目使用 Google Guice（非 Spring），阅读时请将示例中的 Spring 注解映射为 Guice 等价物：`@Component`/`@Service`/`@Repository` → Guice `@Singleton` + Module 绑定；`@Autowired` → `@Inject`；`@Configuration`/`@Bean` → Guice `Module.configure()`。SOLID 原则本身与框架无关。

> SOLID 是面向对象编程和设计的五个基本原则，由 Robert C. Martin（Uncle Bob）提出。遵循这些原则能让代码更易于维护、扩展和理解。

> **Token 估算**：约 8K tokens

---

## 目录

- [SOLID 设计原则](#solid-设计原则)
  - [目录](#目录)
  - [概述](#概述)
  - [1. 单一职责原则 SRP](#1-单一职责原则-srp)
    - [SRP 定义](#srp-定义)
    - [违反 SRP 的典型信号](#违反-srp-的典型信号)
    - [反例：万能 UserService](#反例万能-userservice)
    - [正例：职责拆分](#正例职责拆分)
    - [衡量标准：变化轴](#衡量标准变化轴)
    - [SRP 在 Spring 项目中的实践](#srp-在-spring-项目中的实践)
  - [2. 开闭原则 OCP](#2-开闭原则-ocp)
    - [OCP 定义](#ocp-定义)
    - [核心手段：面向抽象编程](#核心手段面向抽象编程)
    - [反例：if-else 蔓延](#反例if-else-蔓延)
    - [正例：策略模式](#正例策略模式)
    - [Spring 中的 OCP 实践](#spring-中的-ocp-实践)
    - [OCP 的边界](#ocp-的边界)
  - [3. 里氏替换原则 LSP](#3-里氏替换原则-lsp)
    - [LSP 定义](#lsp-定义)
    - [LSP 的四条核心约束](#lsp-的四条核心约束)
    - [反例：正方形不是长方形](#反例正方形不是长方形)
    - [正例：组合替代继承](#正例组合替代继承)
    - [Java 中的 LSP 契约](#java-中的-lsp-契约)
  - [4. 接口隔离原则 ISP](#4-接口隔离原则-isp)
    - [ISP 定义](#isp-定义)
    - [核心思想：胖接口之害](#核心思想胖接口之害)
    - [反例：巨型接口](#反例巨型接口)
    - [正例：接口拆分](#正例接口拆分)
    - [ISP 在 Spring 项目中的实践](#isp-在-spring-项目中的实践)
    - [接口拆分粒度经验值](#接口拆分粒度经验值)
  - [5. 依赖倒置原则 DIP](#5-依赖倒置原则-dip)
    - [DIP 定义](#dip-定义)
    - [反例：高层依赖低层](#反例高层依赖低层)
    - [正例：依赖倒置](#正例依赖倒置)
    - [DIP 与 DI（依赖注入）的关系](#dip-与-di依赖注入的关系)
    - [DIP 在分层架构中的实践](#dip-在分层架构中的实践)
  - [6. 原则之间的关系](#6-原则之间的关系)
    - [关系图](#关系图)
    - [协同效应](#协同效应)
  - [7. SOLID 与反模式](#7-solid-与反模式)
    - [过度设计](#过度设计)
    - [分析瘫痪](#分析瘫痪)
    - [总结：适度原则](#总结适度原则)

---

## 概述

| 缩写 | 原则                      | 一句话概括                           |
| ------ | --------------------------- | -------------------------------------- |
| S    | 单一职责原则 SRP           | 一个类只做一件事                     |
| O    | 开闭原则 OCP               | 对扩展开放，对修改关闭               |
| L    | 里氏替换原则 LSP           | 子类必须能完全替换父类               |
| I    | 接口隔离原则 ISP           | 不应强迫客户端依赖它不需要的接口     |
| D    | 依赖倒置原则 DIP           | 依赖抽象而非具体实现                 |

---

## 1. 单一职责原则 SRP

> Single Responsibility Principle — A class should have only one reason to change.（单一职责原则 — 一个类应该有且仅有一个引起它变化的原因）

### SRP 定义

**一个类应该有且仅有一个引起它变化的原因。** 换句话说，一个类只负责一件事。

这里的"职责"不是指"一个类只做一件事（一个方法）"，而是指**这个类变化的原因只有一个**。

### 违反 SRP 的典型信号

- 类的方法数量膨胀（超过 10 个 public 方法就要审视）
- 修改一个功能时，总是不小心影响另一个功能
- 类的注释用"和"、"以及"连接多个职责
- 很难给类起一个准确的名字

### 反例：万能 UserService

```java
public class UserService {

    public void register(User user) {
        // 校验
        if (StringUtils.isBlank(user.getName())) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        // 持久化
        userDao.insert(user);
        // 发送欢迎邮件
        emailSender.send(user.getEmail(), "欢迎注册", "恭喜您成为我们的用户！");
        // 记录操作日志
        logDao.insert(new OperationLog("REGISTER", user.getId()));
    }
}
```

**问题分析**：`register` 方法同时承担了校验、持久化、邮件发送、日志记录四个职责。当邮件发送逻辑变更或日志格式调整时，都需要修改 `UserService`，违反了 SRP。

### 正例：职责拆分

```java
// 1. 输入校验
@Component
public class UserValidator {
    public void validate(User user) {
        if (StringUtils.isBlank(user.getName())) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (!user.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}

// 2. 持久化
@Repository
public class UserDao {
    public void insert(User user) {
        // SQL insert
    }
}

// 3. 邮件通知
@Component
public class WelcomeEmailSender {
    public void send(User user) {
        emailSender.send(user.getEmail(), "欢迎注册", "恭喜您成为我们的用户！");
    }
}

// 4. 操作日志
@Component
public class OperationLogRecorder {
    public void record(String action, Long userId) {
        logDao.insert(new OperationLog(action, userId));
    }
}

// 5. 编排层 — 只做编排
@Service
public class UserRegistrationService {
    private final UserValidator validator;
    private final UserDao userDao;
    private final WelcomeEmailSender emailSender;
    private final OperationLogRecorder logRecorder;

    public UserRegistrationService(UserValidator validator, UserDao userDao,
                                   WelcomeEmailSender emailSender, OperationLogRecorder logRecorder) {
        this.validator = validator;
        this.userDao = userDao;
        this.emailSender = emailSender;
        this.logRecorder = logRecorder;
    }

    public void register(User user) {
        validator.validate(user);
        userDao.insert(user);
        emailSender.send(user);
        logRecorder.record("REGISTER", user.getId());
    }
}
```

### 衡量标准：变化轴

判断是否违反 SRP 的核心问题是：**这个类会因为多少种不同的原因被修改？**

| 变化来源       | 谁提出变更       | 影响什么类             |
| --------------- | ----------------- | ---------------------- |
| 业务规则变更    | 产品经理         | `UserValidator`      |
| 数据库选型切换  | DBA / 架构师      | `UserDao`            |
| 邮件模板调整    | 运营             | `WelcomeEmailSender` |
| 日志格式变更    | 运维             | `OperationLogRecorder` |

每个类的变更都来自 **同一类角色**。

### SRP 在 Spring 项目中的实践

- **Controller** 只做参数接收和结果返回
- **Service** 只做业务编排
- **Repository / DAO** 只做数据访问
- **Converter / Mapper** 只做对象转换
- **Validator** 只做校验

```java
@RestController
public class UserController {

    private final UserRegistrationService registrationService;

    @PostMapping("/users")
    public Result<Long> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = UserConverter.toDomain(request);
        Long userId = registrationService.register(user);
        return Result.success(userId);
    }
}
```

---

## 2. 开闭原则 OCP

> Open/Closed Principle — Software entities should be open for extension, but closed for modification.

### OCP 定义

**对扩展开放，对修改关闭。** 当需求变化时，应该通过 **新增代码** 来扩展功能，而不是修改已有的代码。

### 核心手段：面向抽象编程

实现 OCP 的关键是 **抽象化**：

- 定义接口 / 抽象类作为契约
- 不同实现对应不同策略
- 新增功能 = 新增实现类

### 反例：if-else 蔓延

```java
public class PaymentService {

    public void pay(String type, BigDecimal amount) {
        if ("alipay".equals(type)) {
            // 支付宝支付逻辑
            System.out.println("支付宝支付：" + amount);
        } else if ("wechat".equals(type)) {
            // 微信支付逻辑
            System.out.println("微信支付：" + amount);
        } else if ("unionpay".equals(type)) {
            // 银联支付逻辑
            System.out.println("银联支付：" + amount);
        } else {
            throw new IllegalArgumentException("不支持的支付方式");
        }
    }
}
```

**问题**：每次新增支付方式（如 Apple Pay），都要修改 `PaymentService` 的 if-else 分支，违反 OCP。

### 正例：策略模式

```java
// 抽象
public interface PaymentStrategy {
    void pay(BigDecimal amount);
    String getType();
}

// 实现
@Component
public class AlipayStrategy implements PaymentStrategy {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("支付宝支付：" + amount);
    }

    @Override
    public String getType() {
        return "alipay";
    }
}

@Component
public class WechatPayStrategy implements PaymentStrategy {
    @Override
    public void pay(BigDecimal amount) {
        System.out.println("微信支付：" + amount);
    }

    @Override
    public String getType() {
        return "wechat";
    }
}

// 工厂 — 自动发现所有 PaymentStrategy 实现
@Service
public class PaymentService {
    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentService(List<PaymentStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getType, Function.identity()));
    }

    public void pay(String type, BigDecimal amount) {
        PaymentStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的支付方式：" + type);
        }
        strategy.pay(amount);
    }
}
```

**新增 Apple Pay 时**：只需新增一个 `ApplePayStrategy` 实现类，`PaymentService` 零修改，满足 OCP。

### Spring 中的 OCP 实践

Spring 框架本身就是 OCP 的典范：

| 扩展点 | 扩展方式 | 无需修改源码 |
| ------------------------- | ---------------------- | ------------- |
| `BeanPostProcessor` | 实现接口 | ✅ |
| `HandlerInterceptor` | 实现接口 | ✅ |
| `ApplicationListener` | 实现接口 + 注册 Bean | ✅ |
| `Converter<S, T>` | 实现接口 | ✅ |

```java
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("Request URI: " + request.getRequestURI());
        return true;
    }
}
```

### OCP 的边界

不过 OCP 不是绝对的——预测未来变化是有成本的：

- **高概率变化点**（如支付方式、消息通道）→ 值得预先抽象
- **低概率变化点** → 先保持简单，等第二次变化发生时再抽象（Rule of Three）

---

## 3. 里氏替换原则 LSP

> Liskov Substitution Principle — Subtypes must be substitutable for their base types.

### LSP 定义

**子类必须能够完全替换其父类，且程序的行为不变。** 如果程序中使用父类的地方，换成子类后出现错误或行为不一致，就违反了 LSP。

### LSP 的四条核心约束

| 约束                   | 说明                                           |
| ----------------------- | ----------------------------------------------- |
| 前置条件不能加强         | 子类不能对输入参数提出比父类更严格的要求              |
| 后置条件不能减弱         | 子类不能返回比父类更弱的结果或抛更宽泛的异常           |
| 不变量必须保持一致        | 父类的内部约束（如 `age >= 0`）子类必须保持          |
| 历史约束                | 子类不应引入父类没有的状态变更方式                    |

### 反例：正方形不是长方形

这是 LSP 的经典反例。

```java
class Rectangle {
    protected int width;
    protected int height;

    public void setWidth(int width)  { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public int getArea() { return width * height; }
}

class Square extends Rectangle {
    @Override
    public void setWidth(int width) {
        this.width = width;
        this.height = width;   // 破坏了 Rectangle 的不变量
    }

    @Override
    public void setHeight(int height) {
        this.width = height;
        this.height = height;
    }
}
```

**测试**：

```java
void resize(Rectangle r) {
    r.setWidth(5);
    r.setHeight(10);
    assert r.getArea() == 50;  // Rectangle 通过，Square 失败（100）
}
```

`Square` 虽然数学上是"is-a"矩形，但代码行为上不能替换 `Rectangle` — 违反 LSP。

### 正例：组合替代继承

```java
interface Shape {
    int getArea();
}

class Rectangle implements Shape {
    private final int width;
    private final int height;

    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getArea() { return width * height; }
}

class Square implements Shape {
    private final int side;

    public Square(int side) { this.side = side; }

    @Override
    public int getArea() { return side * side; }
}
```

### Java 中的 LSP 契约

```java
public class SavingsAccount extends BankAccount {

    @Override
    public void withdraw(BigDecimal amount) {
        // ❌ 违反 LSP：前置条件加强了（余额必须 > 1000）
        if (balance.compareTo(new BigDecimal("1000")) < 0) {
            throw new InsufficientBalanceException("储蓄账户最低余额不低于1000");
        }
        super.withdraw(amount);
    }
}

// ✅ 方案：在父类设计时就考虑扩展点
public abstract class BankAccount {
    public void withdraw(BigDecimal amount) {
        if (!canWithdraw(amount)) {
            throw new InsufficientBalanceException();
        }
        // 扣款逻辑
    }

    protected abstract boolean canWithdraw(BigDecimal amount);  // 扩展点
}
```

---

## 4. 接口隔离原则 ISP

> Interface Segregation Principle — Clients should not be forced to depend on interfaces they do not use.

### ISP 定义

**不应强迫客户端依赖它不需要的接口。** 接口应该小而专注，"胖接口"应该拆分成多个"瘦接口"。

### 核心思想：胖接口之害

- 实现类被迫实现不需要的方法，产生大量空实现或 `throw new UnsupportedOperationException()`
- 调用方看到无关方法，增加理解成本
- 修改胖接口时，即使只关心其中一个方法，所有实现类都需要重新编译

### 反例：巨型接口

```java
public interface Worker {
    void work();
    void eat();
    void sleep();
}
```

```java
public class HumanWorker implements Worker {
    @Override public void work()  { System.out.println("人工作"); }
    @Override public void eat()   { System.out.println("人吃饭"); }
    @Override public void sleep() { System.out.println("人睡觉"); }
}

public class RobotWorker implements Worker {
    @Override public void work()  { System.out.println("机器人工作"); }
    @Override public void eat()   {
        throw new UnsupportedOperationException("机器人不吃东西");  // ❌
    }
    @Override public void sleep() {
        throw new UnsupportedOperationException("机器人不睡觉");    // ❌
    }
}
```

### 正例：接口拆分

```java
public interface Workable {
    void work();
}

public interface Eatable {
    void eat();
}

public interface Sleepable {
    void sleep();
}
```

```java
public class HumanWorker implements Workable, Eatable, Sleepable {
    @Override public void work()  { System.out.println("人工作"); }
    @Override public void eat()   { System.out.println("人吃饭"); }
    @Override public void sleep() { System.out.println("人睡觉"); }
}

public class RobotWorker implements Workable {
    @Override public void work()  { System.out.println("机器人工作"); }
}
```

### ISP 在 Spring 项目中的实践

Spring 自身的接口设计是 ISP 的典范：

```java
// Spring 没有做巨型接口，而是细分了多个角色接口
public interface BeanFactory { ... }           // 获取 Bean
public interface ListableBeanFactory { ... }   // 列举 Bean
public interface HierarchicalBeanFactory { ... } // 父子容器
public interface AutowireCapableBeanFactory { ... } // 自动装配

// 不同客户端按需依赖
public class MyClass {
    private final BeanFactory beanFactory;  // 只需要获取 Bean，不依赖 Listable 等
}
```

**DAO 拆分示例**：

```java
public interface UserReadDao {
    User findById(Long id);
    List<User> findByDeptId(Long deptId);
}

public interface UserWriteDao {
    Long insert(User user);
    int update(User user);
    int deleteById(Long id);
}

// 查询服务只依赖读
@Service
public class UserQueryService {
    private final UserReadDao userReadDao;  // 不依赖写操作
}

// 管理服务依赖读写
@Service
public class UserManageService {
    private final UserReadDao userReadDao;
    private final UserWriteDao userWriteDao;
}
```

### 接口拆分粒度经验值

| 接口方法数 | 建议               |
| ----------- | ------------------- |
| 1-3 个    | 合理，角色单一       |
| 4-6 个    | 审视是否有两个职责混入  |
| 7+ 个     | 应该拆分            |

---

## 5. 依赖倒置原则 DIP

> Dependency Inversion Principle — Depend upon abstractions, not concretions.

### DIP 定义

**高层模块不应该依赖低层模块，二者都应该依赖抽象；抽象不应该依赖细节，细节应该依赖抽象。**

两句话概括：

1. 高层模块不应直接依赖低层模块，都通过接口/抽象交互
2. 接口属于高层模块，由高层定义契约

### 反例：高层依赖低层

```java
// 低层模块
public class MySQLDatabase {
    public void connect()     { System.out.println("连接 MySQL"); }
    public void disconnect()  { System.out.println("断开 MySQL"); }
    public List<String> query(String sql) {
        return Arrays.asList("row1", "row2");
    }
}

// 高层模块直接依赖低层 — ❌
public class UserReportService {
    private final MySQLDatabase database = new MySQLDatabase();  // 硬编码依赖

    public List<String> generateReport() {
        database.connect();
        List<String> data = database.query("SELECT * FROM users");
        database.disconnect();
        return data;
    }
}
```

**问题**：

- `UserReportService` 直接依赖 `MySQLDatabase`，无法切换到 PostgreSQL
- 单元测试难以进行（必须真有 MySQL）
- 低层数据库的修改可能破坏高层业务逻辑

### 正例：依赖倒置

```java
// 抽象 — 由高层模块定义契约
public interface Database {
    void connect();
    void disconnect();
    List<String> query(String sql);
}

// 低层实现
public class MySQLDatabase implements Database {
    @Override public void connect()     { System.out.println("连接 MySQL"); }
    @Override public void disconnect()  { System.out.println("断开 MySQL"); }
    @Override public List<String> query(String sql) {
        return Arrays.asList("row1", "row2");
    }
}

public class PostgreSQLDatabase implements Database {
    @Override public void connect()     { System.out.println("连接 PostgreSQL"); }
    @Override public void disconnect()  { System.out.println("断开 PostgreSQL"); }
    @Override public List<String> query(String sql) {
        return Arrays.asList("row3", "row4");
    }
}

// 高层模块 — 依赖抽象
public class UserReportService {
    private final Database database;

    public UserReportService(Database database) {  // 依赖注入
        this.database = database;
    }

    public List<String> generateReport() {
        database.connect();
        List<String> data = database.query("SELECT * FROM users");
        database.disconnect();
        return data;
    }
}
```

### DIP 与 DI（依赖注入）的关系

| 概念       | 说明                                                         |
| ----------- | ------------------------------------------------------------- |
| DIP（原则） | 设计层面的指导原则 — 应该依赖抽象而非具体                        |
| DI（手段）  | 实现 DIP 的具体技术 — 通过外部容器将具体实现注入到依赖方          |
| IoC（容器） | 控制反转容器（如 Spring IoC）— 负责管理对象的创建和依赖注入       |

```java
// DIP 是思想：
//   高层模块 → 接口 ← 低层模块

// DI 是实现：
@Configuration
public class DatabaseConfig {
    @Bean
    public Database database() {
        return new MySQLDatabase();  // 切换数据库只需改这一处
    }
}

// Spring IoC 完成注入
@Service
public class UserReportService {
    private final Database database;

    public UserReportService(Database database) {  // @Autowired 自动注入
        this.database = database;
    }
}
```

### DIP 在分层架构中的实践

```plaintext
┌──────────────────────────┐
│   Controller（Web 层）     │
├──────────────────────────┤
│   Service（业务层）         │ ← 定义接口：UserRepository（属于业务层）
├──────────────────────────┤
│   Repository Interface   │ ← 抽象在这里
├──────────────────────────┤
│   JdbcUserRepository     │ ← 实现依赖接口（细节依赖抽象）
└──────────────────────────┘
```

```java
// 业务层定义接口（高层定义契约）
package com.example.user.service;

public interface UserRepository {
    User findById(Long id);
    void save(User user);
}

// 数据层实现接口（低层依赖抽象）
package com.example.user.repository;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public User findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", User.class, id);
    }

    @Override
    public void save(User user) {
        jdbcTemplate.update("INSERT INTO users (name, email) VALUES (?, ?)",
                user.getName(), user.getEmail());
    }
}
```

---

## 6. 原则之间的关系

### 关系图

```plaintext
  SRP（单一职责）
   ↓ 拆分出小类      OCP（开闭）
   ↓ 小类易扩展 ──→ 对扩展开放
   ↓
  LSP（里氏替换）    ISP（接口隔离）
   ↓ 保证替换         ↓ 细化接口
   ↓                 ↓
  DIP（依赖倒置）←────┘
   依赖抽象接口
```

### 协同效应

- **SRP → OCP**：单一职责的类更容易通过新增替换（策略模式），自然满足 OCP
- **LSP → OCP**：如果子类不能替换父类，就谈不上"对扩展开放"
- **ISP → DIP**：小而专注的接口更容易被依赖和替换
- **OCP + DIP**：通过抽象隔离变化，是 OCP 的核心手段

---

## 7. SOLID 与反模式

### 过度设计

初学者容易走向另一个极端——为每个类都套用接口，导致类爆炸。

```java
// ❌ 过度设计示例
public interface IUserNameValidator {
    boolean validate(String name);
}

public class UserNameValidator implements IUserNameValidator {
    @Override
    public boolean validate(String name) {
        return name != null && name.length() >= 2;
    }
}
```

这里只有一个实现，且未来大概率没有第二个实现。为它抽取接口只是增加了无意义的间接层。

**判断标准**：当前只有一个实现 + 没有可预见的变化 → 不需要接口。

### 分析瘫痪

SOLID 是指导原则，不是教条。不要在设计阶段花大量时间追求"完美符合 SOLID"。实践中的建议：

1. 先写最简单的能工作的代码
2. 当第二次修改同一块代码时，重构为符合 SOLID
3. 测试是重构的安全网

### 总结：适度原则

| 原则   | 过度表现               | 适度标准                       |
| -------- | ---------------------- | ------------------------------ |
| SRP    | 把类拆得太碎，一个类只有一个方法 | 类有清晰的名字，方法不超过 10 个 |
| OCP    | 所有地方都预留扩展点         | 只在高概率变化点做抽象           |
| LSP    | 不敢用继承                  | 继承前先做替换性测试             |
| ISP    | 每个方法都是一个接口         | 接口 1-3 个方法，只多角色拆分     |
| DIP    | 所有类都配一个接口           | 只在有多实现可能性时抽象          |

---

> **一句话记住 SOLID**：用 SRP 拆解职责，用 OCP 拥抱变化，用 LSP 保证继承安全，用 ISP 精细化依赖，用 DIP 反转控制。
