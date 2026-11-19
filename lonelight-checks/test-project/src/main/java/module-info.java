// 检查：ModuleDirectiveOrder
// 模块指令故意打乱顺序，供扫描和整理后复扫。
module lonelight.test.project {
    uses java.util.function.Supplier;

    exports base;

    provides java.util.function.Supplier with base.OrderNumberSupplier;

    opens project.api;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires io.swagger.v3.oas.annotations;
    requires jakarta.servlet;
    requires jakarta.validation;
    requires java.naming;
    requires java.sql;
    requires java.validation;
    requires java.xml;
    requires jdk.httpserver;
    requires jdk.unsupported;
    requires org.slf4j;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires spring.webmvc;
}
