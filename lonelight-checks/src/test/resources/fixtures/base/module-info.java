// 检查：ModuleDirectiveOrder
// 把 requires、exports、opens、uses、provides 依次整理到正确位置
module example {
    uses java.lang.Runnable;
    provides java.lang.Runnable with base.Work;
    exports base;
    opens base.internal;
    requires java.logging;
}
