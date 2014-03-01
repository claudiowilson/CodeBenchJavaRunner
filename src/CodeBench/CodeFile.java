package CodeBench;

public class CodeFile {
    private String code;
    private String fileName;

    public CodeFile(String fileName, String code) {
        this.code = code.replace("\r","");
        this.fileName=fileName;
    }

    public String getCode() {
        return code;
    }

    public String getFileName() {
        return fileName+".java";
    }

    public String getProgramName() {
        return fileName;
    }
}
