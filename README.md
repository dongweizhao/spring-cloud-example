# 概述
下面我们将学习如何创建多个 Spring boot 微服务以及如何使用 RestTemplate 类在多个微服务之间进行同步通信。

微服务通信有两种风格：

- 同步通讯
- 异步通信

# 同步通讯

在同步通信的情况下，客户端发送请求并等待服务的响应。这里重要的一点是协议（HTTP/HTTPS）是同步的，客户端代码只有在收到 HTTP 服务器响应时才能继续其任务。

例如，Microservice1 作为客户端发送请求并等待 Microservice2 的响应。

我们可以使用 RestTemplate 或 WebClient 或 Spring Cloud Open Feign 库来同步通信多个微服务。
# 异步通信
在异步通信的情况下，客户端发送请求并且不等待服务的响应。客户端将继续执行其任务 - 它不会等待服务的响应。

例如，  微服务1 作为客户端发送请求，并不等待 微服务2 的响应。

我们可以使用RabbitMQ和Apache Kafka等消息代理在多个微服务之间进行异步通信。

# 我们需要做什么
下面我们将创建两个微服务，例如部门服务和用户服务，并且我们将从用户服务到部门服务进行 REST API 调用以获取特定的用户部门。

![](https://files.mdnice.com/user/35072/d3f82c15-7ed5-4a55-a9c4-0bae96218703.png)

并且每个微服务创建一个单独的 MySQL 数据库。
在 IntelliJ IDEA 中创建并设置两个 Spring boot 项目作为两个微服务。

## 1.创建DepartmentService微服务
首先 在 IntelliJ IDEA 中创建并设置部门服务Spring boot 项目

1.在IntelliJ IDEA中创建并设置spring boot项目（部门服务）
我们使用 springinitializr创建一个 Spring boot 项目。
请查看下面的屏幕截图，在使用 springinitializr创建 Spring Boot 应用程序时输入详细信息 ：

点击“GENERATE”按钮以 zip 文件形式下载 Spring boot 项目。解压zip文件并在IntelliJ IDEA中导入Spring boot项目。
pom.xml文件供参考：
``` xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.7.17</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>io.wz</groupId>
	<artifactId>department-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>department-service</name>
	<description>department-service</description>
	<properties>
		<java.version>1.8</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>

```
### DepartmentService - 配置 MySQL 数据库
由于我们使用 MySQL 作为数据库，因此我们需要配置 URL、用户名和密码，以便 Spring boot 在启动时可以与数据库建立连接。
打开 `src/main/resources/application.properties` 文件并向其中添加以下属性：
```
spring.datasource.url=jdbc:mysql://localhost:3306/department_db
spring.datasource.username=root
spring.datasource.password=Mysql@123

spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```
这里注意修改的MySQL密码，另外在 MySQL 中创建一个名为Department_db的数据库 。
不需要创建任何表。Hibernate 将根据我们将在下一步中定义的Department实体自动创建这些表 。这是通过属性 spring.jpa.hibernate.ddl-auto = update 自动实现的。

### DepartmentService - 创建部门 JPA 实体
``` java
package io.wz.departmentservice.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String departmentName;
    private String departmentAddress;
    private String departmentCode;
}
```
### DepartmentService - 创建 Spring Data JPA 存储库
``` java
package io.wz.departmentservice.repository;

import io.wz.departmentservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
```
### DepartmentService - 创建服务层
DepartmentService
``` java
package io.wz.departmentservice.service;

import io.wz.departmentservice.entity.Department;

public interface DepartmentService {
    Department saveDepartment(Department department);

    Department getDepartmentById(Long departmentId);
}
```
DepartmentServiceImpl 类
``` java
package io.wz.departmentservice.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.wz.departmentservice.entity.Department;
import io.wz.departmentservice.repository.DepartmentRepository;
import io.wz.departmentservice.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private DepartmentRepository departmentRepository;

    @Override
    public Department saveDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department getDepartmentById(Long departmentId) {
        return departmentRepository.findById(departmentId).get();
    }
}

```
### DepartmentService - 创建Controller层
DepartmentController
```java
package io.wz.departmentservice.controller;

import lombok.AllArgsConstructor;
import io.wz.departmentservice.entity.Department;
import io.wz.departmentservice.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/departments")
@AllArgsConstructor
public class DepartmentController {

    private DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<Department> saveDepartment(@RequestBody Department department){
        Department savedDepartment = departmentService.saveDepartment(department);
        return new ResponseEntity<>(savedDepartment, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable("id") Long departmentId){
        Department department = departmentService.getDepartmentById(departmentId);
        return ResponseEntity.ok(department);
    }
}
```

### DepartmentService - 启动 Spring Boot 应用程序
我们可以通过两种方式启动独立的 Spring boot 应用程序。
1. 从应用程序的根目录并键入以下命令来运行它 -
```
$ mvn spring-boot:run
```
2. 从 IDE 中，将`DepartmentServiceApplication.main()`方法作为独立 Java 类运行，该方法将在端口 8080 上启动嵌入式 Tomcat 服务器并将浏览器指向 http://localhost:8080/。

### DepartmentService - 使用 Postman 客户端测试 REST API
保存部门 REST API：

![](https://files.mdnice.com/user/35072/778e77fd-e78e-4d09-890f-7d04da7b62c7.png)

### 获取单个部门 REST API：

![](https://files.mdnice.com/user/35072/a0dbe6b9-69b2-40e1-b063-9ad1b254fc77.png)

## 2.创建UserService微服务
我们首先在 IntelliJ IDEA 中创建并设置`UserService`Spring boot 项目
1.在IntelliJ IDEA中创建并设置spring boot项目（用户服务）
使用 springinitializr创建一个 Spring boot 项目。
请参阅下面的屏幕截图，在使用 springinitializr创建 Spring Boot 应用程序时输入详细信息 ：

![](https://files.mdnice.com/user/35072/7227352e-f23e-40af-bc2f-d844629e5e04.png)

单击“GENRATE”按钮以 zip 文件形式下载 Spring boot 项目。解压zip文件并在IntelliJ IDEA中导入Spring boot项目。
pom.xml 文件供参考：
``` xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.7.17</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>io.wz</groupId>
	<artifactId>user-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>user-service</name>
	<description>user-service</description>
	<properties>
		<java.version>1.8</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>

```
### UserService - 配置 MySQL 数据库
打开 `src/main/resources/application.properties` 文件并向其中添加以下属性：
``` yaml
spring.datasource.url=jdbc:mysql://localhost:3306/employee_db
spring.datasource.username=root
spring.datasource.password=Mysql@123

spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```
这里注意修改的MySQL密码，另外在 MySQL 中创建一个名为employee_db的数据库 。
不需要创建任何表。Hibernate 将根据我们将在下一步中定义的User实体自动创建这些表 。这是通过属性 spring.jpa.hibernate.ddl-auto = update 自动实现的。
### UserService - 更改服务器端口
注意，部门服务 Spring boot 项目运行在默认的 tomcat 服务器端口 8080 上。
对于用户服务，我们需要使用以下属性将嵌入式 tomcat 服务器端口更改为 8081：
``` yaml
server.port = 8081
```
### UserService - 创建用户 JPA 实体
``` java
package io.wz.userservice.entity;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @Column(nullable = false, unique = true)
    private String email;
    private String departmentId;
}
```
### UserService - 创建 Spring Data JPA 存储库
```java
package io.wz.userservice.repository;

import io.wz.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
```
### UserService - 创建 DTO 类
> DepartmentDto
```java
package io.wz.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto {
    private Long id;
    private String departmentName;
    private String departmentAddress;
    private String departmentCode;
}
```
> UserDto
``` java
package io.wz.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
```
> ResponseDto
```java
package io.wz.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {
    private DepartmentDto department;
    private UserDto user;
}
```
### UserService - 将 RestTemplate 配置为 Spring Bean
将 RestTemplate 类配置为 Spring bean，以便我们可以注入并使用它。
```java
package io.wz.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
```
### UserService - 创建服务层
> 用户服务接口
```java
package io.wz.userservice.service;

import io.wz.userservice.dto.ResponseDto;
import io.wz.userservice.entity.User;

public interface UserService {
    User saveUser(User user);

    ResponseDto getUser(Long userId);
}
```
> UserServiceImpl class
```java
package io.wz.userservice.service.impl;

import lombok.AllArgsConstructor;
import io.wz.userservice.dto.DepartmentDto;
import io.wz.userservice.dto.ResponseDto;
import io.wz.userservice.dto.UserDto;
import io.wz.userservice.entity.User;
import io.wz.userservice.repository.UserRepository;
import io.wz.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private RestTemplate restTemplate;

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public ResponseDto getUser(Long userId) {

        ResponseDto responseDto = new ResponseDto();
        User user = userRepository.findById(userId).get();
        UserDto userDto = mapToUser(user);

        ResponseEntity<DepartmentDto> responseEntity = restTemplate
                .getForEntity("http://localhost:8080/api/departments/" + user.getDepartmentId(),
                DepartmentDto.class);

        DepartmentDto departmentDto = responseEntity.getBody();

        System.out.println(responseEntity.getStatusCode());

        responseDto.setUser(userDto);
        responseDto.setDepartment(departmentDto);

        return responseDto;
    }

    private UserDto mapToUser(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }
}
```
请注意，以上我们使用RestTemplate对部门服务进行 REST API 调用：
```java
ResponseEntity<DepartmentDto> responseEntity = restTemplate
                .getForEntity("http://localhost:8080/api/departments/" + user.getDepartmentId(),
                DepartmentDto.class);
```
### UserService - 创建控制器层：UserController
```java
package io.wz.userservice.controller;

import lombok.AllArgsConstructor;
import io.wz.userservice.dto.ResponseDto;
import io.wz.userservice.entity.User;
import io.wz.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
@AllArgsConstructor
public class UserController {

    private UserService userService;

    @PostMapping
    public ResponseEntity<User> saveUser(@RequestBody User user){
        User savedUser = userService.saveUser(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @GetMapping("{id}")
    public ResponseEntity<ResponseDto> getUser(@PathVariable("id") Long userId){
        ResponseDto responseDto = userService.getUser(userId);
        return ResponseEntity.ok(responseDto);
    }
}
```
### UserService - 启动 Spring Boot 应用程序
我们可以通过两种方式启动独立的 Spring boot 应用程序。
1. 从应用程序的根目录并键入以下命令来运行它 -
```shell
$ mvn spring-boot:run
```
2. 在 IDE 中，将 `UserServiceApplication.main()` 方法作为独立 Java 类运行，该方法将在端口 8080 上启动嵌入式 Tomcat 服务器并将浏览器指向 `http://localhost:8081/`。
### UserService - 使用 Postman 客户端测试 REST API
#### 保存用户 REST API：

![](https://files.mdnice.com/user/35072/9a26d4a3-36d4-4ce4-b6a0-c155c54683a0.png)
#### 获取用户 REST API：

![](https://files.mdnice.com/user/35072/245402d1-a389-4f8a-82ff-d668b8d6ce94.png)
请注意，响应包含用户的部门。这说明我们已成功从 UserService 到 DepartmentService 进行 REST API 调用。

## 结论
在本教程中，我们学习了如何创建多个 Spring boot 微服务以及如何使用RestTemplate类在多个微服务之间进行同步通信。

从 5.0 开始，  RestTemplate 类处于维护模式，很快就会被弃用。因此 Spring 团队推荐使用 `org.springframework.web.reactive.client.WebClient` ，它具有现代 API 并支持同步、异步和流场景,下一篇文章继续讲解



