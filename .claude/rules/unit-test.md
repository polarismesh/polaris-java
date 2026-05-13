# 单元测试规范

本文件为 polaris-java 项目级单元测试规范。所有新增/修改的单元测试代码均须遵循。

## 测试框架

- **JUnit 4**
    - 核心注解：@Test, @Before
    - 运行器：@RunWith(MockitoJUnitRunner.class)（推荐）

## Mock 框架

- **Mockito**
    - 核心注解：@Mock, @InjectMocks
    - 核心方法：when(), verify()
- 对外部调用、随机数、时间等使用 Mockito 进行 mock
- 不要在测试代码中重新实现被测试方法
- 对静态方法使用mockStatic进行mock

## 测试代码规范

- 注释以简单的中文呈现
- 代码生成完，自动执行设置代码格式、优化import、重新排列代码、删除单元测试类内部的无用方法

## 代码结构

```java
/**
 * Test for {@link 测试类名称}.
 *
 * @author {{git的user.name}}
 */
@RunWith(MockitoJUnitRunner.class)
public class XxxTest {

    // 测试常量定义
    private static final String TEST_VALUE = "test";

    // 测试对象实例
    @InjectMocks
    private Xxx testInstance;

    // 测试前的初始化
    @Before
    public void setUp() {
        // 如有额外初始化可写在这里
    }

    // 具体测试方法
    @Test
    public void testMethod() {
        // 测试实现
    }
}
```

## 测试方法结构

```java

@Test
public void testMethod() {
    // Arrange：准备测试数据和环境

    // Act：执行被测试的方法

    // Assert：验证测试结果
}
```

## 断言使用

- 使用org.assertj.core.api.Assertions下的断言方法
- 使用恰当的断言方法
- 提供清晰的错误信息
- 验证所有关键结果
- 常用断言方法：
  ```java
  assertThat(Predicate<T> actual)
  assertThatCode(ThrowableAssert.ThrowingCallable shouldRaiseOrNotThrowable)
  assertThatThrownBy(ThrowableAssert.ThrowingCallable shouldRaiseThrowable)
  ```

## 静态方法mock

- 使用方法：
  ```
  org.mockito.Mockito#mockStatic(java.lang.Class<T>)
  org.mockito.MockedStatic#when(Verification verification)
  ```
- 实例
  ```
  try (MockedStatic<Xxx> mockedApplicationContextAwareUtils = Mockito.mockStatic(Xxx.class)) {
            // Arrange：准备测试数据和环境
			mockedApplicationContextAwareUtils.when(() -> Xxx.xxmethod(anyString()))
					.thenReturn(xxresult);

			// Act：执行被测试的方法

            // Assert：验证测试结果
		}
  ```

## 私有属性访问

- 私有属性不能直接访问，需要使用反射辅助方法，设置属性为可访问
- 参考代码：
  ```java
    public static <T> T getPrivateField(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        T result = (T) field.get(object);
        return result;
    }

    public static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
  ```

## 复杂数据类型测试

- 对于处理多种数据类型的方法，应该为每种类型创建单独的测试方法
- 测试方法名应该清晰地表明测试的数据类型
- 对于 JSON 数据的测试，应该包含：
    1. 正确格式数据测试
    2. 错误格式数据测试
    3. 边界条件测试（空值、null 等）
- 示例命名：
  ```java
  testMethodName_BasicTypes()     // 基本类型测试
  testMethodName_ComplexTypes()   // 复杂类型测试
  testMethodName_InvalidData()    // 非法数据测试
  ```
- 尝试构造有实际意义的测试数据

```java
// 示例：使用有意义的测试数据
@Test
public void testMethod() {
    // 使用有意义的业务标识符
    statistics.setTenantId("tenant123");
    statistics.setBizId("biz123");
    // 使用符合实际场景的数据
    statistics.setUniqueId(123L);
}
```

- **Setter 方法调用规范**：
    - 在准备测试数据（Arrange 阶段）时，如果需要调用多个 setter 方法为对象设置属性，请**避免使用链式调用**。
    - 每个 setter 方法的调用都应该**独立成行**，以提高代码的可读性和可维护性。
    - **反例（不推荐）**：
      ```java
      // 不推荐的链式调用，可读性较差
      statistics.setTenantId("tenant123")
                .setBizId("biz123")
                .setUniqueId(123L);
      ```
    - **正例（推荐）**：
      ```java
      // 推荐的独立调用，每个属性设置一目了然
      statistics.setTenantId("tenant123");
      statistics.setBizId("biz123");
      statistics.setUniqueId(123L);
      ```

## 注释规范

- 在编写每个用例时都要添加说明注释

```java
/**
 * 测试方法的中文注释
 * 包含以下内容：
 * 1. 测试目的：说明这个测试用例要验证什么
 * 2. 测试场景：说明测试的具体场景
 * 3. 验证内容：说明需要验证的具体内容
 */
```

## 导入依赖

- 需要把原始类中的所有 import 语句拷贝到测试类中，确保测试类中不会缺少引入必要的依赖类
- 需要优化import导入

## Git

- 自动将新生成代码文件进行git add