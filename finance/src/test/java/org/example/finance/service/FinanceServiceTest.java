package org.example.finance.service;
import org.example.finance.dto.StatisticsDTO;
import org.example.finance.model.FinanceRecord;
import org.example.finance.repository.FinanceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FinanceServiceTest {

    @Mock
    private FinanceRecordRepository financeRecordRepository;

    @Mock
    private UserService userService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FinanceService financeService;

    private FinanceRecord sampleExpenseRecord;
    private FinanceRecord sampleIncomeRecord;
    private List<FinanceRecord> recordList;
    private StatisticsDTO statisticsDTO;
    private Map<String, Double> totals;
    private List<Map<String, Object>> incomeByCategory;
    private List<Map<String, Object>> expenseByCategory;

    @BeforeEach
    void setUp() {
        // 创建测试数据
        sampleExpenseRecord = new FinanceRecord();
        sampleExpenseRecord.setId(1);
        sampleExpenseRecord.setCategory("食品");
        sampleExpenseRecord.setExpense(100.0);
        sampleExpenseRecord.setRecordDate(LocalDate.now());
        sampleExpenseRecord.setNotes("午餐费用");

        sampleIncomeRecord = new FinanceRecord();
        sampleIncomeRecord.setId(2);
        sampleIncomeRecord.setCategory("工资");
        sampleIncomeRecord.setIncome(5000.0);
        sampleIncomeRecord.setRecordDate(LocalDate.now());
        sampleIncomeRecord.setNotes("月薪");

        recordList = Arrays.asList(sampleExpenseRecord, sampleIncomeRecord);

        // 设置统计数据
        totals = new HashMap<>();
        totals.put("totalIncome", 5000.0);
        totals.put("totalExpense", 1000.0);

        incomeByCategory = new ArrayList<>();
        Map<String, Object> incomeCat = new HashMap<>();
        incomeCat.put("category", "工资");
        incomeCat.put("total", 5000.0);
        incomeByCategory.add(incomeCat);

        expenseByCategory = new ArrayList<>();
        Map<String, Object> expenseCat = new HashMap<>();
        expenseCat.put("category", "食品");
        expenseCat.put("total", 1000.0);
        expenseByCategory.add(expenseCat);
    }

    @Test
    @DisplayName("测试获取所有记录 - 使用assertEquals断言")
    void testGetAllRecords() {
        // 设置模拟行为
        when(financeRecordRepository.findAll()).thenReturn(recordList);

        // 执行测试
        List<FinanceRecord> result = financeService.getAllRecords();

        // 断言1: assertEquals - 验证结果大小和内容
        assertEquals(2, result.size(), "应该返回2条记录");
        assertEquals("食品", result.get(0).getCategory(), "第一条记录类别应为食品");
        assertEquals(5000.0, result.get(1).getIncome(), "第二条记录收入应为5000.0");

        // 验证方法调用
        verify(financeRecordRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("测试根据ID获取记录 - 使用assertTrue和assertFalse断言")
    void testGetRecordById() {
        // 设置模拟行为
        when(financeRecordRepository.findById(1)).thenReturn(Optional.of(sampleExpenseRecord));
        when(financeRecordRepository.findById(999)).thenReturn(Optional.empty());

        // 执行测试
        Optional<FinanceRecord> foundRecord = financeService.getRecordById(1);
        Optional<FinanceRecord> notFoundRecord = financeService.getRecordById(999);

        // 断言2: assertTrue - 验证记录存在
        assertTrue(foundRecord.isPresent(), "ID为1的记录应该存在");

        // 断言3: assertFalse - 验证记录不存在
        assertFalse(notFoundRecord.isPresent(), "ID为999的记录不应该存在");
    }

    @Test
    @DisplayName("测试保存记录 - 使用assertNotNull和assertSame断言")
    void testSaveRecord() {
        // 设置模拟行为
        when(financeRecordRepository.save(any(FinanceRecord.class))).thenReturn(sampleExpenseRecord);

        // 执行测试
        FinanceRecord savedRecord = financeService.saveRecord(sampleExpenseRecord);

        // 断言4: assertNotNull - 验证返回对象不为空
        assertNotNull(savedRecord, "保存的记录不应为空");

        // 断言5: assertSame - 验证返回的是同一个对象
        assertSame(sampleExpenseRecord, savedRecord, "应该返回相同的对象引用");
    }

    @Test
    @DisplayName("测试删除不存在的记录 - 使用assertFalse断言")
    void testDeleteNonExistingRecord() {
        // 设置模拟行为
        when(financeRecordRepository.existsById(999)).thenReturn(false);

        // 执行测试
        boolean result = financeService.deleteRecord(999);

        // 使用assertFalse断言
        assertFalse(result, "删除不存在的记录应返回false");
    }

    @Test
    @DisplayName("测试获取统计信息 - 使用assertAll组合断言")
    void testGetStatistics() {
        // 设置模拟行为
        when(financeRecordRepository.getTotalIncomeAndExpense()).thenReturn(totals);
        when(financeRecordRepository.getIncomeByCategory()).thenReturn(incomeByCategory);
        when(financeRecordRepository.getExpenseByCategory()).thenReturn(expenseByCategory);

        // 执行测试
        StatisticsDTO result = financeService.getStatistics();

        // 断言6: assertAll - 组合多个断言一起执行
        assertAll("统计数据验证",
                () -> assertEquals(5000.0, result.getTotalIncome(), "总收入应为5000.0"),
                () -> assertEquals(1000.0, result.getTotalExpense(), "总支出应为1000.0"),
                () -> assertEquals(4000.0, result.getBalance(), "结余应为4000.0"),
                () -> assertEquals(1, result.getIncomeByCategory().size(), "收入类别应有1个"),
                () -> assertEquals(1, result.getExpenseByCategory().size(), "支出类别应有1个")
        );
    }

    @Test
    @DisplayName("测试检查月度支出限制 - 使用assertInstanceOf和assertDoesNotThrow断言")
    void testCheckMonthlyExpenseLimit() {
        // 设置模拟行为
        when(financeRecordRepository.getCurrentMonthTotalExpense()).thenReturn(1500.0);
        when(userService.getMonthlyExpenseLimit()).thenReturn(1000.0);

        // 断言7: assertDoesNotThrow - 验证方法不会抛出异常
        Map<String, Object> result = assertDoesNotThrow(() ->
                        financeService.checkMonthlyExpenseLimit(),
                "检查月度限制不应抛出异常"
        );

        // 断言8: assertInstanceOf - 验证返回类型
        assertInstanceOf(Map.class, result, "结果应该是Map类型");

        // 其他断言
        assertTrue((Boolean) result.get("exceededLimit"), "应该超出限制");
        assertEquals(1500.0, result.get("currentMonthExpense"), "当前月支出应为1500.0");
    }

    @Test
    @DisplayName("测试保存记录并检查限制 - 使用assertThrows断言")
    void testSaveRecordAndCheckLimitWithException() {
        // 设置模拟行为 - 模拟保存时发生异常
        when(financeRecordRepository.save(any(FinanceRecord.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        // 断言9: assertThrows - 验证方法会抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            financeService.saveRecordAndCheckLimit(sampleExpenseRecord);
        }, "保存记录时应抛出异常");

        // 验证异常消息
        assertEquals("数据库错误", exception.getMessage(), "异常消息应匹配");
    }

    @Test
    @DisplayName("测试清除所有数据 - 使用verify和assertDoesNotThrow断言")
    void testClearAllData() {
        // 配置模拟行为
        doNothing().when(financeRecordRepository).deleteAll();
        when(financeRecordRepository.findAll()).thenReturn(new ArrayList<>());

        // 断言方法不抛出异常
        assertDoesNotThrow(() -> financeService.clearAllData(), "清除数据不应抛出异常");

        // 验证方法调用
        verify(financeRecordRepository, times(1)).deleteAll();
        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}