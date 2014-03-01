package CodeBench;

public class CodeFile {
    private String code;
    private String fileName;
    private String fileExtension;

    public CodeFile(String fileName, String code, String fileExtension) {
        this.code = code.replace("\r","");
        this.fileName=fileName;
        this.fileExtension=fileExtension;
    }

    public String getCode() {
        return code;
    }

    public String getFileName() {
        return fileName+"."+fileExtension;
    }

    public String getProgramName() {
        return fileName;
    }
}
