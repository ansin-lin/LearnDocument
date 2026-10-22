// 读取 CSV 时使用逐行读取器。
import java.io.BufferedReader;
// 明确把练习 CSV 当作 UTF-8 文本。
import java.nio.charset.StandardCharsets;
// 提供按路径打开文件的工具。
import java.nio.file.Files;
// 表示输入文件的路径。
import java.nio.file.Path;

/** Training-only external program. It does NOT run Spring Batch or write to a database. */
// 此类只模拟简化 CSV 校验与计数，不执行真实导入。
public final class MockSalesImport {
    // 工具类不创建对象，入口只使用下方的 main 方法。
    private MockSalesImport() { }

    // Java 进程从这里开始，接收日期和 CSV 路径两个参数。
    public static void main(String[] args) {
        // 参数必须恰好两个，且日期参数必须是八位数字。
        if (args.length != 2 || !args[0].matches("[0-9]{8}")) {
            // 向标准错误报告参数格式。
            System.err.println("ERROR invalid arguments: YYYYMMDD CSV_FILE required");
            // 参数错误时以状态 2 结束 Java 进程。
            System.exit(2);
        // 结束参数判断。
        }
        // 第二个参数是待读取的 CSV 路径。
        Path input = Path.of(args[1]);
        // 从零开始统计有效数据行，不把标题行算进去。
        int count = 0;
        // 以 UTF-8 打开文件，并在结束时自动关闭读取器。
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            // 第一行必须与本案例约定的两个字段名完全一致。
            if (!"sale_id,amount".equals(reader.readLine())) {
                // 报告标题格式错误。
                System.err.println("ERROR invalid CSV header");
                // 数据格式错误以状态 10 结束。
                System.exit(10);
            // 结束标题检查。
            }
            // 暂存每次读到的一行文字。
            String line;
            // 逐行读取，直到文件末尾返回 null。
            while ((line = reader.readLine()) != null) {
                // 简化示例按逗号切为两个字段；保留尾部空字段供校验。
                String[] fields = line.split(",", -1);
                // 要求两列、S 加数字的销售 ID、纯数字金额。
                if (fields.length != 2 || !fields[0].matches("S[0-9]+")
                        // 续接上一行的“或”条件：金额必须是数字。
                        || !fields[1].matches("[0-9]+")) {
                    // 报告出错的数据行号，标题占第 1 行。
                    System.err.println("ERROR invalid CSV data at row " + (count + 2));
                    // 数据校验失败以状态 10 结束。
                    System.exit(10);
                // 结束单行校验。
                }
                // 当前行有效，计数加一。
                count++;
            // 结束逐行读取。
            }
            // 只有标题而无数据的文件不算有效输入。
            if (count == 0) {
                // 报告空数据集。
                System.err.println("ERROR CSV has no data rows");
                // 以数据错误状态 10 结束。
                System.exit(10);
            // 结束数据行数检查。
            }
            // 所有简化检查通过，输出日期和数据行数。
            System.out.println("MOCK_IMPORT date=" + args[0] + " rows=" + count);
        // 打开或读取文件时发生异常，进入错误处理。
        } catch (Exception ex) {
            // 只报告异常类型，不输出输入路径或堆栈。
            System.err.println("ERROR cannot read CSV: " + ex.getClass().getSimpleName());
            // 读取失败也按本案例的数据处理失败状态 10 返回。
            System.exit(10);
        // 结束读取与异常处理。
        }
    // 结束 main 方法；正常走到这里时 Java 返回 0。
    }
// 结束模拟程序类。
}
