// 检查：FileTabCharacter, NewlineAtEndOfFile, LineLength, RegexpSingleline#noTrailingWhitespace
// 检查：OneTopLevelClass, NoLineWrap, PackageName
// 包名跨行且含大写，另有 Tab、长行、尾随空格和缺失的文件末尾换行
package
Base;

class FileLayout {
    // 长行样例：abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 同一行累积过多内容时，编辑器右侧阅读和差异审查都会更困难，因此这里特意保留一整段说明来触发行宽提示；分行后应只消除这一处行宽诊断
	int count;  
}
class AnotherTopLevel { }