package org.example.fina;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 财务记录数据模型
 */
public class FinanceRecord {
    private final SimpleIntegerProperty id;
    private final SimpleDoubleProperty income;
    private final SimpleDoubleProperty expense;
    private final SimpleStringProperty category ;
    private final SimpleStringProperty date;
    private final SimpleStringProperty note;

    public FinanceRecord(int id, double income, double expense, String category,String date, String note) {
        this.id = new SimpleIntegerProperty(id);
        this.income = new SimpleDoubleProperty(income);
        this.expense = new SimpleDoubleProperty(expense);
        this.category = new SimpleStringProperty(category);
        this.date = new SimpleStringProperty(date);
        this.note = new SimpleStringProperty(note);
    }

    public int getId() {
        return id.get();
    }

    public double getIncome() {
        return income.get();
    }

    public void setIncome(double income) {
        this.income.set(income);
    }

    public double getExpense() {
        return expense.get();
    }

    public void setExpense(double expense) {
        this.expense.set(expense);
    }
    public String getCategory() {return category.get();}
    public void setCategory(String category) {this.category.set(category);}

    public String getDate() {
        return date.get();
    }

    public void setDate(String date) {
        this.date.set(date);
    }

    public String getNote() {
        return note.get();
    }

    public void setNote(String note) {
        this.note.set(note);
    }
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleDoubleProperty incomeProperty() { return income; }
    public SimpleDoubleProperty expenseProperty() { return expense; }
    public SimpleStringProperty categoryProperty() { return category; }
    public SimpleStringProperty dateProperty() { return date; }
    public SimpleStringProperty noteProperty() { return note; }
}
