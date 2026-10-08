/* ============================================================
   Bank Management System
   Language : Java (Swing GUI)
   Description:
   A Java GUI application for complete bank-account lifecycle
   management: account creation, modification, closure,
   deposits, withdrawals, and multi-criteria search/sort.
   ============================================================ */

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* ---------------------------------------------------------
   Account: represents a single bank account
   --------------------------------------------------------- */
class Account {
    private final int accountNumber;
    private String holderName;
    private String accountType;   // "Savings" or "Current"
    private String mobile;
    private double balance;
    private final String openDate;
    private String status;        // "Active" or "Closed"

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Account(int accountNumber, String holderName, String accountType,
                   String mobile, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.accountType = accountType;
        this.mobile = mobile;
        this.balance = balance;
        this.openDate = LocalDate.now().format(FMT);
        this.status = "Active";
    }

    public int getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public String getAccountType() { return accountType; }
    public String getMobile() { return mobile; }
    public double getBalance() { return balance; }
    public String getOpenDate() { return openDate; }
    public String getStatus() { return status; }

    public void setHolderName(String v) { this.holderName = v; }
    public void setAccountType(String v) { this.accountType = v; }
    public void setMobile(String v) { this.mobile = v; }
    public void setBalance(double v) { this.balance = v; }
    public void setStatus(String v) { this.status = v; }

    public boolean isActive() { return "Active".equals(status); }
}

/* ---------------------------------------------------------
   AccountManager: all business logic (no GUI code here),
   kept separate so it is easy to follow and to test.
   --------------------------------------------------------- */
class AccountManager {
    private final List<Account> accounts = new ArrayList<>();
    private int nextAccountNumber = 1001;

    public Account createAccount(String holderName, String accountType,
                                 String mobile, double openingBalance) {
        if (openingBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        Account acc = new Account(nextAccountNumber++, holderName, accountType, mobile, openingBalance);
        accounts.add(acc);
        return acc;
    }

    public Account findByAccountNumber(int accNumber) {
        for (Account a : accounts) {
            if (a.getAccountNumber() == accNumber) return a;
        }
        return null;
    }

    public boolean modifyAccount(int accNumber, String newName, String newType, String newMobile) {
        Account a = findByAccountNumber(accNumber);
        if (a == null || !a.isActive()) return false;
        if (newName != null && !newName.isBlank()) a.setHolderName(newName);
        if (newType != null && !newType.isBlank()) a.setAccountType(newType);
        if (newMobile != null && !newMobile.isBlank()) a.setMobile(newMobile);
        return true;
    }

    public boolean closeAccount(int accNumber) {
        Account a = findByAccountNumber(accNumber);
        if (a == null || !a.isActive()) return false;
        if (a.getBalance() != 0) {
            throw new IllegalStateException("Account balance must be 0 before closing. Current balance: " + a.getBalance());
        }
        a.setStatus("Closed");
        return true;
    }

    public void deposit(int accNumber, double amount) {
        Account a = findByAccountNumber(accNumber);
        if (a == null) throw new IllegalArgumentException("Account not found.");
        if (!a.isActive()) throw new IllegalStateException("Cannot deposit into a closed account.");
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        a.setBalance(a.getBalance() + amount);
    }

    public void withdraw(int accNumber, double amount) {
        Account a = findByAccountNumber(accNumber);
        if (a == null) throw new IllegalArgumentException("Account not found.");
        if (!a.isActive()) throw new IllegalStateException("Cannot withdraw from a closed account.");
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if (amount > a.getBalance()) throw new IllegalStateException("Insufficient balance.");
        a.setBalance(a.getBalance() - amount);
    }

    public List<Account> getAll() {
        return new ArrayList<>(accounts);
    }

    /** Multi-criteria search: every non-empty / non-"Any" criterion must match (AND logic). */
    public List<Account> search(String nameContains, Integer accNumber, String type,
                                String status, Double minBalance, Double maxBalance) {
        List<Account> result = new ArrayList<>();
        for (Account a : accounts) {
            if (nameContains != null && !nameContains.isBlank()
                    && !a.getHolderName().toLowerCase().contains(nameContains.toLowerCase())) continue;
            if (accNumber != null && a.getAccountNumber() != accNumber) continue;
            if (type != null && !type.equals("Any") && !type.equals(a.getAccountType())) continue;
            if (status != null && !status.equals("Any") && !status.equals(a.getStatus())) continue;
            if (minBalance != null && a.getBalance() < minBalance) continue;
            if (maxBalance != null && a.getBalance() > maxBalance) continue;
            result.add(a);
        }
        return result;
    }

    /** Sort a list of accounts by field name, ascending or descending. */
    public List<Account> sort(List<Account> list, String field, boolean ascending) {
        List<Account> copy = new ArrayList<>(list);
        Comparator<Account> cmp;
        switch (field) {
            case "Account Number": cmp = Comparator.comparingInt(Account::getAccountNumber); break;
            case "Name":           cmp = Comparator.comparing(Account::getHolderName, String.CASE_INSENSITIVE_ORDER); break;
            case "Balance":        cmp = Comparator.comparingDouble(Account::getBalance); break;
            case "Open Date":      cmp = Comparator.comparing(Account::getOpenDate); break;
            default:                cmp = Comparator.comparingInt(Account::getAccountNumber);
        }
        if (!ascending) cmp = cmp.reversed();
        copy.sort(cmp);
        return copy;
    }
}

/* ---------------------------------------------------------
   AccountTableModel: bridges the account list and the JTable
   --------------------------------------------------------- */
class AccountTableModel extends AbstractTableModel {
    private final String[] columns = {
            "Acc. Number", "Holder Name", "Type", "Mobile", "Balance", "Open Date", "Status"
    };
    private List<Account> data = new ArrayList<>();

    public void setData(List<Account> data) {
        this.data = data;
        fireTableDataChanged();
    }

    public List<Account> getData() { return data; }

    public Account getAccountAt(int row) { return data.get(row); }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int col) { return columns[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        Account a = data.get(row);
        switch (col) {
            case 0: return a.getAccountNumber();
            case 1: return a.getHolderName();
            case 2: return a.getAccountType();
            case 3: return a.getMobile();
            case 4: return String.format("%.2f", a.getBalance());
            case 5: return a.getOpenDate();
            case 6: return a.getStatus();
            default: return "";
        }
    }
}

/* ---------------------------------------------------------
   BankManagementSystem: main Swing GUI
   --------------------------------------------------------- */
public class BankManagementSystem extends JFrame {

    private final AccountManager manager = new AccountManager();
    private final AccountTableModel tableModel = new AccountTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusBar = new JLabel("Ready.");

    public BankManagementSystem() {
        super("Bank Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        loadSampleAccounts();

        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        refreshTable(manager.getAll());
    }

    private void loadSampleAccounts() {
        manager.createAccount("Esraa Ibrahim", "Savings", "01028088709", 5000);
        manager.createAccount("Ahmed Mostafa", "Current", "01112223344", 12500);
        manager.createAccount("Mariam Youssef", "Savings", "01276543210", 800);
    }

    private JToolBar buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        bar.add(makeButton("New Account", e -> onCreateAccount()));
        bar.add(makeButton("Modify", e -> onModifyAccount()));
        bar.add(makeButton("Close Account", e -> onCloseAccount()));
        bar.addSeparator();
        bar.add(makeButton("Deposit", e -> onDeposit()));
        bar.add(makeButton("Withdraw", e -> onWithdraw()));
        bar.addSeparator();
        bar.add(makeButton("Search", e -> onSearch()));
        bar.add(makeButton("Sort", e -> onSort()));
        bar.add(makeButton("Refresh", e -> refreshTable(manager.getAll())));

        return bar;
    }

    private JButton makeButton(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.addActionListener(listener);
        return b;
    }

    private void refreshTable(List<Account> list) {
        tableModel.setData(list);
        statusBar.setText(list.size() + " account(s) shown.");
    }

    private Integer getSelectedAccountNumber() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an account from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return tableModel.getAccountAt(row).getAccountNumber();
    }

    /* ---------------- Create ---------------- */
    private void onCreateAccount() {
        JTextField nameField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Savings", "Current"});
        JTextField mobileField = new JTextField();
        JTextField balanceField = new JTextField("0");

        Object[] form = {
                "Holder name:", nameField,
                "Account type:", typeBox,
                "Mobile:", mobileField,
                "Opening balance:", balanceField
        };

        int result = JOptionPane.showConfirmDialog(this, form, "New Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) throw new IllegalArgumentException("Holder name cannot be empty.");
            double balance = Double.parseDouble(balanceField.getText().trim());

            Account acc = manager.createAccount(name, (String) typeBox.getSelectedItem(),
                    mobileField.getText().trim(), balance);
            refreshTable(manager.getAll());
            statusBar.setText("Account " + acc.getAccountNumber() + " created successfully.");
        } catch (NumberFormatException ex) {
            showError("Opening balance must be a valid number.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /* ---------------- Modify ---------------- */
    private void onModifyAccount() {
        Integer accNum = getSelectedAccountNumber();
        if (accNum == null) return;
        Account current = manager.findByAccountNumber(accNum);

        JTextField nameField = new JTextField(current.getHolderName());
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Savings", "Current"});
        typeBox.setSelectedItem(current.getAccountType());
        JTextField mobileField = new JTextField(current.getMobile());

        Object[] form = {
                "Holder name:", nameField,
                "Account type:", typeBox,
                "Mobile:", mobileField
        };

        int result = JOptionPane.showConfirmDialog(this, form, "Modify Account #" + accNum,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        boolean ok = manager.modifyAccount(accNum, nameField.getText().trim(),
                (String) typeBox.getSelectedItem(), mobileField.getText().trim());
        if (ok) {
            refreshTable(manager.getAll());
            statusBar.setText("Account " + accNum + " updated successfully.");
        } else {
            showError("Could not update: account not found or closed.");
        }
    }

    /* ---------------- Close ---------------- */
    private void onCloseAccount() {
        Integer accNum = getSelectedAccountNumber();
        if (accNum == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Close account #" + accNum + "? The balance must be 0.",
                "Confirm Closure", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = manager.closeAccount(accNum);
            if (ok) {
                refreshTable(manager.getAll());
                statusBar.setText("Account " + accNum + " closed.");
            } else {
                showError("Could not close: account not found or already closed.");
            }
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    /* ---------------- Deposit / Withdraw ---------------- */
    private void onDeposit() {
        Integer accNum = getSelectedAccountNumber();
        if (accNum == null) return;
        String input = JOptionPane.showInputDialog(this, "Deposit amount for account #" + accNum + ":");
        if (input == null) return;
        try {
            double amount = Double.parseDouble(input.trim());
            manager.deposit(accNum, amount);
            refreshTable(manager.getAll());
            statusBar.setText(String.format("Deposited %.2f into account %d.", amount, accNum));
        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void onWithdraw() {
        Integer accNum = getSelectedAccountNumber();
        if (accNum == null) return;
        String input = JOptionPane.showInputDialog(this, "Withdraw amount for account #" + accNum + ":");
        if (input == null) return;
        try {
            double amount = Double.parseDouble(input.trim());
            manager.withdraw(accNum, amount);
            refreshTable(manager.getAll());
            statusBar.setText(String.format("Withdrew %.2f from account %d.", amount, accNum));
        } catch (NumberFormatException ex) {
            showError("Please enter a valid amount.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    /* ---------------- Multi-criteria search ---------------- */
    private void onSearch() {
        JTextField nameField = new JTextField();
        JTextField accNumField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Any", "Savings", "Current"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Any", "Active", "Closed"});
        JTextField minBalField = new JTextField();
        JTextField maxBalField = new JTextField();

        Object[] form = {
                "Name contains:", nameField,
                "Account number (exact):", accNumField,
                "Account type:", typeBox,
                "Status:", statusBox,
                "Min balance:", minBalField,
                "Max balance:", maxBalField
        };

        int result = JOptionPane.showConfirmDialog(this, form, "Search Accounts (multi-criteria)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        try {
            Integer accNum = accNumField.getText().trim().isEmpty() ? null : Integer.parseInt(accNumField.getText().trim());
            Double minBal = minBalField.getText().trim().isEmpty() ? null : Double.parseDouble(minBalField.getText().trim());
            Double maxBal = maxBalField.getText().trim().isEmpty() ? null : Double.parseDouble(maxBalField.getText().trim());

            List<Account> results = manager.search(nameField.getText().trim(), accNum,
                    (String) typeBox.getSelectedItem(), (String) statusBox.getSelectedItem(), minBal, maxBal);
            refreshTable(results);
        } catch (NumberFormatException ex) {
            showError("Account number / balance fields must be valid numbers.");
        }
    }

    /* ---------------- Sort ---------------- */
    private void onSort() {
        JComboBox<String> fieldBox = new JComboBox<>(new String[]{"Account Number", "Name", "Balance", "Open Date"});
        JComboBox<String> orderBox = new JComboBox<>(new String[]{"Ascending", "Descending"});

        Object[] form = { "Sort by:", fieldBox, "Order:", orderBox };
        int result = JOptionPane.showConfirmDialog(this, form, "Sort Accounts",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        boolean ascending = orderBox.getSelectedItem().equals("Ascending");
        List<Account> sorted = manager.sort(tableModel.getData(), (String) fieldBox.getSelectedItem(), ascending);
        refreshTable(sorted);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new BankManagementSystem().setVisible(true);
        });
    }
}
