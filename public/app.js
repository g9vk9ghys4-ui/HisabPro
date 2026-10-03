// HisabPro PWA - Offline-First Local IndexedDB Application

// --- IndexedDB Configuration ---
const DB_NAME = 'HisabProDB';
const DB_VERSION = 1;
let db = null;

// Global state
let currentView = 'parties';
let selectedFilter = 'customers';
let activePartyId = null;
let deferredInstallPrompt = null;

// Initial Seed Data
const INITIAL_PARTIES = [
  { id: 1, name: "Sharma Electronics", phone: "9876543210", address: "Main Market, Shop #12", isSupplier: false, collectionDateMillis: Date.now() - 86400000 },
  { id: 2, name: "Rajesh Kirana Store", phone: "9812345678", address: "Gali No. 4, Subhash Nagar", isSupplier: false, collectionDateMillis: Date.now() },
  { id: 3, name: "Priya Boutique", phone: "9988776655", address: "Opp. City Hospital", isSupplier: false, collectionDateMillis: Date.now() + 86400000 * 2 },
  { id: 4, name: "Amit Hardware Traders", phone: "9123456780", address: "Industrial Area Phase 1", isSupplier: true, collectionDateMillis: null },
  { id: 5, name: "Sunita Textiles", phone: "9765432109", address: "Cloth Market, Gate 2", isSupplier: false, collectionDateMillis: Date.now() + 86400000 * 5 }
];

const INITIAL_TRANSACTIONS = [
  { id: 1, partyId: 1, type: "GAVE", amount: 4500.0, description: "LED TV wall mount & cables", paymentMethod: "Cash", dateMillis: Date.now() - 86400000 * 3 },
  { id: 2, partyId: 1, type: "GOT", amount: 1500.0, description: "Partial payment received", paymentMethod: "UPI", dateMillis: Date.now() - 86400000 * 2 },
  { id: 3, partyId: 2, type: "GAVE", amount: 1850.0, description: "Monthly grocery supplies", paymentMethod: "Cash", dateMillis: Date.now() - 86400000 * 4 },
  { id: 4, partyId: 3, type: "GAVE", amount: 3200.0, description: "Designer suit fabric", paymentMethod: "UPI", dateMillis: Date.now() - 86400000 },
  { id: 5, partyId: 4, type: "GOT", amount: 12000.0, description: "Stock delivery - steel pipes", paymentMethod: "Bank Transfer", dateMillis: Date.now() - 86400000 * 5 },
  { id: 6, partyId: 4, type: "GAVE", amount: 5000.0, description: "Advance paid for stock", paymentMethod: "Bank Transfer", dateMillis: Date.now() - 86400000 * 2 }
];

const INITIAL_CASHBOOK = [
  { id: 1, type: "IN", amount: 1500.0, description: "Cash received from Sharma Electronics", dateMillis: Date.now() - 86400000 },
  { id: 2, type: "OUT", amount: 450.0, description: "Shop electricity bill", dateMillis: Date.now() - 86400000 },
  { id: 3, type: "IN", amount: 2800.0, description: "Direct retail counter sales", dateMillis: Date.now() }
];

const INITIAL_EXPENSES = [
  { id: 1, category: "Electricity", amount: 450.0, description: "Monthly shop power bill", dateMillis: Date.now() - 86400000 },
  { id: 2, category: "Transport", amount: 350.0, description: "Auto rickshaw stock delivery", dateMillis: Date.now() }
];

const INITIAL_INCOMES = [
  { id: 1, category: "Sales", amount: 2800.0, description: "Counter sales", dateMillis: Date.now() }
];

// --- Database Initialization ---
function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (e) => {
      const dbInstance = e.target.result;
      if (!dbInstance.objectStoreNames.contains('parties')) {
        dbInstance.createObjectStore('parties', { keyPath: 'id', autoIncrement: true });
      }
      if (!dbInstance.objectStoreNames.contains('transactions')) {
        dbInstance.createObjectStore('transactions', { keyPath: 'id', autoIncrement: true });
      }
      if (!dbInstance.objectStoreNames.contains('cashbook')) {
        dbInstance.createObjectStore('cashbook', { keyPath: 'id', autoIncrement: true });
      }
      if (!dbInstance.objectStoreNames.contains('expenses')) {
        dbInstance.createObjectStore('expenses', { keyPath: 'id', autoIncrement: true });
      }
      if (!dbInstance.objectStoreNames.contains('incomes')) {
        dbInstance.createObjectStore('incomes', { keyPath: 'id', autoIncrement: true });
      }
      if (!dbInstance.objectStoreNames.contains('settings')) {
        dbInstance.createObjectStore('settings', { keyPath: 'key' });
      }
    };

    request.onsuccess = (e) => {
      db = e.target.result;
      seedDataIfNeeded().then(resolve);
    };

    request.onerror = (e) => reject(e.target.error);
  });
}

function getAllFromStore(storeName) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly');
    const store = tx.objectStore(storeName);
    const request = store.getAll();
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function addItemToStore(storeName, item) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const request = store.add(item);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function deleteItemFromStore(storeName, key) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const request = store.delete(key);
    request.onsuccess = () => resolve(true);
    request.onerror = () => reject(request.error);
  });
}

// Toast notification helper
function showToast(message = "Deleted successfully") {
  const toast = document.getElementById('toastNotification');
  const toastMsg = document.getElementById('toastMessage');
  if (toast && toastMsg) {
    toastMsg.textContent = message;
    toast.classList.add('show');
    clearTimeout(window._toastTimer);
    window._toastTimer = setTimeout(() => {
      toast.classList.remove('show');
    }, 2500);
  }
}

// Translations Dictionary
const TRANSLATIONS = {
  en: {
    khataOverview: "Net Khata Balance",
    youGet: "You'll Get (Udhar)",
    youGive: "You'll Give (Payable)",
    partiesNav: "Parties",
    cashbookNav: "Cashbook",
    expensesNav: "Expenses",
    remindersNav: "Reminders",
    reportsNav: "Reports"
  },
  hi: {
    khataOverview: "कुल खाता शेष",
    youGet: "लेना है (उधार)",
    youGive: "देना है (बाकी)",
    partiesNav: "खाता",
    cashbookNav: "रोकड़ बही",
    expensesNav: "खर्चे",
    remindersNav: "तगादा",
    reportsNav: "रिपोर्ट"
  },
  bn: {
    khataOverview: "মোট খাতা ব্যালেন্স",
    youGet: "আপনি পাবেন (বাকি)",
    youGive: "আপনাকে দিতে হবে",
    partiesNav: "খাতা",
    cashbookNav: "ক্যাশ বই",
    expensesNav: "খরচ",
    remindersNav: "তাগাদা",
    reportsNav: "রিপোর্ট"
  },
  gu: {
    khataOverview: "કુલ ખાતા બાકી",
    youGet: "લેવાના છે (ઉધાર)",
    youGive: "આપવાના છે",
    partiesNav: "ખાતાવહી",
    cashbookNav: "રોકડ મેળ",
    expensesNav: "ખર્ચ",
    remindersNav: "યાદ અપાવો",
    reportsNav: "રિપોર્ટ"
  }
};

function applyLanguage(lang = 'en') {
  const t = TRANSLATIONS[lang] || TRANSLATIONS.en;
  const overviewTitle = document.querySelector('.overview-title');
  if (overviewTitle) overviewTitle.textContent = t.khataOverview;

  const subTitles = document.querySelectorAll('.overview-sub-title');
  if (subTitles.length >= 2) {
    subTitles[0].textContent = t.youGet;
    subTitles[1].textContent = t.youGive;
  }

  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(n => {
    const navKey = n.getAttribute('data-nav');
    const span = n.querySelector('span');
    if (span && navKey) {
      if (navKey === 'parties') span.textContent = t.partiesNav;
      if (navKey === 'cashbook') span.textContent = t.cashbookNav;
      if (navKey === 'expenses') span.textContent = t.expensesNav;
      if (navKey === 'reminders') span.textContent = t.remindersNav;
      if (navKey === 'reports') span.textContent = t.reportsNav;
    }
  });
}

function applyTheme(theme = 'light') {
  if (theme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
  }
  const toggle = document.getElementById('settingThemeToggle');
  if (toggle) toggle.checked = (theme === 'dark');
}

// PIN Lock State
let enteredPinDigits = "";
let correctPin = "";

async function checkAppPinLock() {
  const pinEnabled = await getSetting('pinEnabled', false);
  const appPin = await getSetting('appPin', '');
  if (pinEnabled && appPin && appPin.length === 4) {
    correctPin = appPin;
    enteredPinDigits = "";
    updatePinDots();
    showModal('pinLockModal');
  }
}

function updatePinDots() {
  for (let i = 0; i < 4; i++) {
    const dot = document.getElementById(`pindot-${i}`);
    if (dot) {
      if (i < enteredPinDigits.length) dot.classList.add('filled');
      else dot.classList.remove('filled');
    }
  }
}

function handlePinDigit(digit) {
  if (enteredPinDigits.length < 4) {
    enteredPinDigits += digit;
    updatePinDots();
    if (enteredPinDigits.length === 4) {
      setTimeout(() => {
        if (enteredPinDigits === correctPin) {
          hideModal('pinLockModal');
          showToast("Unlocked successfully");
        } else {
          const errEl = document.getElementById('pinErrorText');
          if (errEl) errEl.textContent = "Incorrect PIN. Try again.";
          setTimeout(() => {
            enteredPinDigits = "";
            updatePinDots();
            if (errEl) errEl.textContent = "";
          }, 800);
        }
      }, 150);
    }
  }
}

// Confirmation modal state & handler
let pendingDeleteAction = null;
let isDeleting = false;

function requestConfirmation(title, message, onConfirm) {
  document.getElementById('confirmDeleteTitle').textContent = title;
  document.getElementById('confirmDeleteMessage').textContent = message;
  pendingDeleteAction = onConfirm;
  showModal('confirmDeleteModal');
}

function setSetting(key, value) {
  return new Promise((resolve, reject) => {
    const tx = db.transaction('settings', 'readwrite');
    const store = tx.objectStore('settings');
    const request = store.put({ key, value });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function getSetting(key, defaultValue = "") {
  return new Promise((resolve) => {
    const tx = db.transaction('settings', 'readonly');
    const store = tx.objectStore('settings');
    const request = store.get(key);
    request.onsuccess = () => {
      resolve(request.result ? request.result.value : defaultValue);
    };
    request.onerror = () => resolve(defaultValue);
  });
}

async function seedDataIfNeeded() {
  const parties = await getAllFromStore('parties');
  if (parties.length === 0) {
    const tx = db.transaction(['parties', 'transactions', 'cashbook', 'expenses', 'incomes', 'settings'], 'readwrite');
    
    INITIAL_PARTIES.forEach(p => tx.objectStore('parties').add(p));
    INITIAL_TRANSACTIONS.forEach(t => tx.objectStore('transactions').add(t));
    INITIAL_CASHBOOK.forEach(c => tx.objectStore('cashbook').add(c));
    INITIAL_EXPENSES.forEach(e => tx.objectStore('expenses').add(e));
    INITIAL_INCOMES.forEach(i => tx.objectStore('incomes').add(i));
    tx.objectStore('settings').add({ key: 'businessName', value: 'Sharma Traders & Hardware' });
    tx.objectStore('settings').add({ key: 'businessPhone', value: '9876501234' });

    return new Promise(res => {
      tx.oncomplete = () => res();
    });
  }
}

// --- Formatting Helpers ---
function formatCurrency(amount) {
  const num = Math.abs(Number(amount) || 0);
  return '₹' + num.toLocaleString('en-IN', { maximumFractionDigits: 2 });
}

function formatDate(timestamp) {
  if (!timestamp) return '';
  const d = new Date(timestamp);
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
}

// Calculate Party Balance
// For Customer: GAVE increases what you get; GOT decreases it. (Positive = You'll Get, Negative = You'll Give)
// For Supplier: GOT increases what you owe (payable); GAVE decreases it.
function calculatePartyBalance(party, txList) {
  let net = 0;
  txList.forEach(t => {
    if (!party.isSupplier) {
      if (t.type === 'GAVE') net += t.amount;
      else net -= t.amount;
    } else {
      if (t.type === 'GOT') net += t.amount;
      else net -= t.amount;
    }
  });
  return net;
}

// --- UI Renderers ---
async function renderPartiesView() {
  const [parties, transactions, businessName] = await Promise.all([
    getAllFromStore('parties'),
    getAllFromStore('transactions'),
    getSetting('businessName', 'My Business')
  ]);

  document.getElementById('headerBusinessName').textContent = businessName;

  // Group transactions by party
  const txByParty = {};
  transactions.forEach(t => {
    if (!txByParty[t.partyId]) txByParty[t.partyId] = [];
    txByParty[t.partyId].push(t);
  });

  let totalYouGet = 0;
  let totalYouGive = 0;

  let customersCount = 0;
  let suppliersCount = 0;

  const partyBalances = parties.map(party => {
    const txs = txByParty[party.id] || [];
    const balance = calculatePartyBalance(party, txs);

    if (party.isSupplier) {
      suppliersCount++;
      if (balance > 0) totalYouGive += balance;
      else if (balance < 0) totalYouGet += Math.abs(balance);
    } else {
      customersCount++;
      if (balance > 0) totalYouGet += balance;
      else if (balance < 0) totalYouGive += Math.abs(balance);
    }

    return { party, balance, txCount: txs.length };
  });

  document.getElementById('countCustomers').textContent = customersCount;
  document.getElementById('countSuppliers').textContent = suppliersCount;
  document.getElementById('totalYouGet').textContent = formatCurrency(totalYouGet);
  document.getElementById('totalYouGive').textContent = formatCurrency(totalYouGive);

  const netBalance = totalYouGet - totalYouGive;
  const netEl = document.getElementById('netKhataAmount');
  if (netBalance >= 0) {
    netEl.textContent = `+${formatCurrency(netBalance)} (Get)`;
    netEl.style.color = '#86EFAC';
  } else {
    netEl.textContent = `-${formatCurrency(Math.abs(netBalance))} (Pay)`;
    netEl.style.color = '#FCA5A5';
  }

  // Filter & Search
  const query = document.getElementById('partySearchInput').value.trim().toLowerCase();
  const filtered = partyBalances.filter(({ party }) => {
    if (selectedFilter === 'customers' && party.isSupplier) return false;
    if (selectedFilter === 'suppliers' && !party.isSupplier) return false;
    if (query) {
      const matchName = party.name.toLowerCase().includes(query);
      const matchPhone = (party.phone || '').toLowerCase().includes(query);
      return matchName || matchPhone;
    }
    return true;
  });

  const container = document.getElementById('partyListContainer');
  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">👥</div>
        <div class="empty-title">No parties found</div>
        <p>Tap '+ Add Party' below to record your first customer or supplier.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(({ party, balance, txCount }) => {
    const initial = party.name.charAt(0).toUpperCase();
    let balanceClass = 'txt-green';
    let balanceLabel = "YOU'LL GET";

    if (party.isSupplier) {
      if (balance > 0) {
        balanceClass = 'txt-red';
        balanceLabel = "YOU'LL GIVE";
      } else if (balance < 0) {
        balanceClass = 'txt-green';
        balanceLabel = "YOU'LL GET";
      } else {
        balanceClass = '';
        balanceLabel = "SETTLED";
      }
    } else {
      if (balance > 0) {
        balanceClass = 'txt-green';
        balanceLabel = "YOU'LL GET";
      } else if (balance < 0) {
        balanceClass = 'txt-red';
        balanceLabel = "YOU'LL GIVE";
      } else {
        balanceClass = '';
        balanceLabel = "SETTLED";
      }
    }

    return `
      <div class="party-item">
        <div style="display: flex; align-items: center; flex: 1; min-width: 0; cursor: pointer;" onclick="openPartyDetail(${party.id})">
          <div class="party-avatar">${initial}</div>
          <div class="party-info">
            <div class="party-name">${escapeHtml(party.name)}</div>
            <div class="party-sub">${party.phone ? escapeHtml(party.phone) : (party.isSupplier ? 'Supplier' : 'Customer')} • ${txCount} entries</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <div class="party-balance" style="cursor: pointer;" onclick="openPartyDetail(${party.id})">
            <div class="balance-amount ${balanceClass}">${formatCurrency(Math.abs(balance))}</div>
            <div class="balance-label ${balanceClass}">${balanceLabel}</div>
          </div>
          <button class="btn-delete-item" onclick="promptDeleteParty(${party.id})" title="Delete ${party.isSupplier ? 'Supplier' : 'Customer'}" aria-label="Delete">🗑️</button>
        </div>
      </div>
    `;
  }).join('');
}

async function renderCashbookView() {
  const cashEntries = await getAllFromStore('cashbook');
  let totalIn = 0;
  let totalOut = 0;

  cashEntries.forEach(c => {
    if (c.type === 'IN') totalIn += c.amount;
    else totalOut += c.amount;
  });

  document.getElementById('totalCashIn').textContent = formatCurrency(totalIn);
  document.getElementById('totalCashOut').textContent = formatCurrency(totalOut);
  document.getElementById('netCashAmount').textContent = formatCurrency(totalIn - totalOut);

  const container = document.getElementById('cashbookListContainer');
  if (cashEntries.length === 0) {
    container.innerHTML = `<div class="empty-state"><p>No cash entries recorded yet.</p></div>`;
    return;
  }

  // Sort descending by date
  const sorted = [...cashEntries].sort((a,b) => b.dateMillis - a.dateMillis);
  container.innerHTML = sorted.map(c => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border);">
      <div style="flex: 1; min-width: 0;">
        <div style="font-weight: 700; font-size: 14px;">${escapeHtml(c.description || (c.type === 'IN' ? 'Cash Received' : 'Cash Paid'))}</div>
        <div style="font-size: 11px; color: var(--text-muted);">${formatDate(c.dateMillis)}</div>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <div style="font-weight: 800; font-size: 15px; color: ${c.type === 'IN' ? 'var(--green)' : 'var(--red)'};">
          ${c.type === 'IN' ? '+' : '-'}${formatCurrency(c.amount)}
        </div>
        <button class="btn-delete-item" onclick="promptDeleteCashbook(${c.id})" title="Delete cash entry">🗑️</button>
      </div>
    </div>
  `).join('');
}

async function renderExpensesView() {
  const [expenses, incomes] = await Promise.all([
    getAllFromStore('expenses'),
    getAllFromStore('incomes')
  ]);

  let totalExp = expenses.reduce((sum, e) => sum + e.amount, 0);
  let totalInc = incomes.reduce((sum, i) => sum + i.amount, 0);

  document.getElementById('totalExpenseAmount').textContent = formatCurrency(totalExp);
  document.getElementById('totalIncomeAmount').textContent = formatCurrency(totalInc);
  document.getElementById('netProfitAmount').textContent = formatCurrency(totalInc - totalExp);

  const allItems = [
    ...expenses.map(e => ({ ...e, isExpense: true })),
    ...incomes.map(i => ({ ...i, isExpense: false }))
  ].sort((a, b) => b.dateMillis - a.dateMillis);

  const container = document.getElementById('expenseListContainer');
  if (allItems.length === 0) {
    container.innerHTML = `<div class="empty-state"><p>No expense or revenue records found.</p></div>`;
    return;
  }

  container.innerHTML = allItems.map(item => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border);">
      <div style="flex: 1; min-width: 0;">
        <div style="font-weight: 700; font-size: 14px;">${escapeHtml(item.category)}</div>
        <div style="font-size: 12px; color: var(--text-muted);">${escapeHtml(item.description || '')} • ${formatDate(item.dateMillis)}</div>
      </div>
      <div style="display: flex; align-items: center; gap: 8px;">
        <div style="font-weight: 800; font-size: 15px; color: ${item.isExpense ? 'var(--red)' : 'var(--green)'};">
          ${item.isExpense ? '-' : '+'}${formatCurrency(item.amount)}
        </div>
        <button class="btn-delete-item" onclick="promptDeleteExpenseIncome(${item.id}, ${item.isExpense})" title="Delete ${item.isExpense ? 'expense' : 'income'}">🗑️</button>
      </div>
    </div>
  `).join('');
}

async function renderRemindersView() {
  const [parties, transactions, businessName] = await Promise.all([
    getAllFromStore('parties'),
    getAllFromStore('transactions'),
    getSetting('businessName', 'HisabPro')
  ]);

  const txByParty = {};
  transactions.forEach(t => {
    if (!txByParty[t.partyId]) txByParty[t.partyId] = [];
    txByParty[t.partyId].push(t);
  });

  const dueCustomers = parties
    .filter(p => !p.isSupplier)
    .map(p => {
      const balance = calculatePartyBalance(p, txByParty[p.id] || []);
      return { party: p, balance };
    })
    .filter(item => item.balance > 0);

  const container = document.getElementById('remindersListContainer');
  if (dueCustomers.length === 0) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">🎉</div><p>No pending customer dues! All clear.</p></div>`;
    return;
  }

  container.innerHTML = dueCustomers.map(({ party, balance }) => {
    const amountStr = formatCurrency(balance);
    const msg = `Dear ${party.name}, your payment of ${amountStr} is pending with ${businessName}. Please pay via UPI or Cash. Thank you!`;
    const waUrl = `https://wa.me/${(party.phone || '').replace(/[^0-9]/g, '')}?text=${encodeURIComponent(msg)}`;

    return `
      <div style="padding: 12px; background: white; border: 1px solid var(--border); border-radius: 10px; margin-bottom: 8px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <div>
            <div style="font-weight: 700; font-size: 15px;">${escapeHtml(party.name)}</div>
            <div style="font-size: 12px; color: var(--text-muted);">${escapeHtml(party.phone || 'No phone')}</div>
          </div>
          <div style="font-weight: 800; font-size: 16px; color: var(--green);">${amountStr}</div>
        </div>
        <div style="display: flex; gap: 8px;">
          <a href="${waUrl}" target="_blank" class="btn btn-whatsapp" style="flex: 1; padding: 8px; font-size: 13px;">💬 Send WhatsApp</a>
          ${party.phone ? `<a href="tel:${party.phone}" class="btn btn-secondary" style="flex: 1; padding: 8px; font-size: 13px;">📞 Call</a>` : ''}
        </div>
      </div>
    `;
  }).join('');
}

async function renderReportsView() {
  const [parties, transactions] = await Promise.all([
    getAllFromStore('parties'),
    getAllFromStore('transactions')
  ]);

  const txByParty = {};
  transactions.forEach(t => {
    if (!txByParty[t.partyId]) txByParty[t.partyId] = [];
    txByParty[t.partyId].push(t);
  });

  let totalDues = 0;
  let totalPayables = 0;
  let customersCount = 0;

  const dueCustomers = [];

  parties.forEach(p => {
    const bal = calculatePartyBalance(p, txByParty[p.id] || []);
    if (!p.isSupplier) {
      customersCount++;
      if (bal > 0) {
        totalDues += bal;
        dueCustomers.push({ party: p, balance: bal });
      } else if (bal < 0) {
        totalPayables += Math.abs(bal);
      }
    } else {
      if (bal > 0) totalPayables += bal;
      else if (bal < 0) totalDues += Math.abs(bal);
    }
  });

  dueCustomers.sort((a,b) => b.balance - a.balance);

  document.getElementById('repTotalCustomers').textContent = customersCount;
  document.getElementById('repTotalDues').textContent = formatCurrency(totalDues);
  document.getElementById('repTotalPayables').textContent = formatCurrency(totalPayables);
  document.getElementById('repTotalTxCount').textContent = transactions.length;

  const debtorsContainer = document.getElementById('topDebtorsContainer');
  if (dueCustomers.length === 0) {
    debtorsContainer.innerHTML = `<p style="font-size: 13px; color: var(--text-muted);">No outstanding customer dues.</p>`;
    return;
  }

  debtorsContainer.innerHTML = dueCustomers.slice(0, 5).map(({ party, balance }) => `
    <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--border);">
      <div>
        <div style="font-weight: 700; font-size: 14px;">${escapeHtml(party.name)}</div>
        <div style="font-size: 11px; color: var(--text-muted);">${escapeHtml(party.phone || '')}</div>
      </div>
      <div style="font-weight: 800; font-size: 15px; color: var(--green);">${formatCurrency(balance)}</div>
    </div>
  `).join('');
}

// --- Party Detail Modal ---
async function openPartyDetail(partyId) {
  activePartyId = partyId;
  const [parties, transactions, businessName] = await Promise.all([
    getAllFromStore('parties'),
    getAllFromStore('transactions'),
    getSetting('businessName', 'HisabPro')
  ]);

  const party = parties.find(p => p.id === partyId);
  if (!party) return;

  const partyTxs = transactions.filter(t => t.partyId === partyId).sort((a,b) => a.dateMillis - b.dateMillis);

  document.getElementById('detailPartyName').textContent = party.name;
  document.getElementById('detailPartyPhone').textContent = party.phone ? `+91 ${party.phone}` : (party.isSupplier ? 'Supplier' : 'Customer');

  // Call Button
  const callBtn = document.getElementById('detailCallBtn');
  if (party.phone) {
    callBtn.href = `tel:${party.phone}`;
    callBtn.style.display = 'flex';
  } else {
    callBtn.style.display = 'none';
  }

  // Calculate balance
  const balance = calculatePartyBalance(party, partyTxs);
  const balEl = document.getElementById('detailPartyBalance');
  balEl.textContent = formatCurrency(Math.abs(balance));
  balEl.className = balance > 0 ? (party.isSupplier ? 'txt-red' : 'txt-green') : (balance < 0 ? (party.isSupplier ? 'txt-green' : 'txt-red') : '');

  // WhatsApp Button
  const waBtn = document.getElementById('detailWhatsAppBtn');
  waBtn.onclick = () => {
    const msg = `Dear ${party.name}, your account balance is ${formatCurrency(Math.abs(balance))} with ${businessName}.`;
    const url = `https://wa.me/${(party.phone || '').replace(/[^0-9]/g, '')}?text=${encodeURIComponent(msg)}`;
    window.open(url, '_blank');
  };

  // Party Delete Buttons (Header Icon & Footer Action Button)
  const deletePartyBtn = document.getElementById('detailDeletePartyBtn');
  if (deletePartyBtn) {
    deletePartyBtn.onclick = (e) => {
      e.stopPropagation();
      promptDeleteParty(party.id);
    };
  }

  const footerDelBtn = document.getElementById('detailDeletePartyActionBtn');
  const footerDelText = document.getElementById('detailDeletePartyActionText');
  if (footerDelBtn) {
    if (footerDelText) {
      footerDelText.textContent = party.isSupplier ? "Delete Supplier & All History" : "Delete Customer & All History";
    }
    footerDelBtn.onclick = (e) => {
      e.stopPropagation();
      promptDeleteParty(party.id);
    };
  }

  // Render Ledger entries with running balance
  let running = 0;
  const ledgerEntries = partyTxs.map(tx => {
    if (!party.isSupplier) {
      if (tx.type === 'GAVE') running += tx.amount;
      else running -= tx.amount;
    } else {
      if (tx.type === 'GOT') running += tx.amount;
      else running -= tx.amount;
    }
    return { ...tx, runningBalance: running };
  }).reverse(); // Show newest on top

  const container = document.getElementById('detailLedgerContainer');
  if (ledgerEntries.length === 0) {
    container.innerHTML = `<div class="empty-state"><p>No transactions recorded yet with ${escapeHtml(party.name)}.</p></div>`;
  } else {
    container.innerHTML = ledgerEntries.map(tx => {
      const isGave = tx.type === 'GAVE';
      return `
        <div style="background: white; border: 1px solid var(--border); border-radius: 8px; padding: 10px; margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="flex: 1; min-width: 0;">
              <div style="font-weight: 700; font-size: 14px;">${escapeHtml(tx.description || (isGave ? 'Udhar / You Gave' : 'Payment / You Got'))}</div>
              <div style="font-size: 11px; color: var(--text-muted);">${formatDate(tx.dateMillis)} • ${tx.paymentMethod || 'Cash'}</div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <div style="text-align: right;">
                <div style="font-size: 16px; font-weight: 800; color: ${isGave ? 'var(--red)' : 'var(--green)'};">
                  ${formatCurrency(tx.amount)}
                </div>
                <div style="font-size: 11px; color: var(--text-muted);">Bal: ${formatCurrency(Math.abs(tx.runningBalance))}</div>
              </div>
              <button class="btn-delete-item" onclick="promptDeleteTransaction(${tx.id})" title="Delete transaction">🗑️</button>
            </div>
          </div>
        </div>
      `;
    }).join('');
  }

  showModal('partyDetailModal');
}

// Helper to escape HTML
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Modal controls
function showModal(id) {
  document.getElementById(id).classList.add('active');
}

function hideModal(id) {
  document.getElementById(id).classList.remove('active');
}

// --- Event Listeners Setup ---
function setupEventListeners() {
  // Navigation
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
      item.classList.add('active');
      const view = item.getAttribute('data-nav');
      switchView(view);
    });
  });

  // Filter chips in Parties
  document.querySelectorAll('.filter-bar .filter-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.filter-bar .filter-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      selectedFilter = chip.getAttribute('data-filter');
      renderPartiesView();
    });
  });

  // Search input
  document.getElementById('partySearchInput').addEventListener('input', () => {
    renderPartiesView();
  });

  // Open Add Party
  document.getElementById('openAddPartyModalBtn').addEventListener('click', () => {
    document.getElementById('partyNameInput').value = '';
    document.getElementById('partyPhoneInput').value = '';
    document.getElementById('partyAddressInput').value = '';
    showModal('addPartyModal');
  });

  // Party Type Toggle in Add Party Modal
  let isNewPartySupplier = false;
  document.getElementById('typeCustomerBtn').addEventListener('click', () => {
    isNewPartySupplier = false;
    document.getElementById('typeCustomerBtn').classList.add('active');
    document.getElementById('typeSupplierBtn').classList.remove('active');
  });
  document.getElementById('typeSupplierBtn').addEventListener('click', () => {
    isNewPartySupplier = true;
    document.getElementById('typeSupplierBtn').classList.add('active');
    document.getElementById('typeCustomerBtn').classList.remove('active');
  });

  // Submit Add Party
  document.getElementById('submitAddPartyBtn').addEventListener('click', async () => {
    const name = document.getElementById('partyNameInput').value.trim();
    const phone = document.getElementById('partyPhoneInput').value.trim();
    const address = document.getElementById('partyAddressInput').value.trim();

    if (!name) {
      alert('Please enter party name');
      return;
    }

    await addItemToStore('parties', {
      name,
      phone,
      address,
      isSupplier: isNewPartySupplier,
      collectionDateMillis: null,
      createdAtMillis: Date.now()
    });

    hideModal('addPartyModal');
    renderPartiesView();
  });

  // Record Transaction buttons in Party Detail
  document.getElementById('detailYouGaveBtn').addEventListener('click', () => {
    openAddTxModal('GAVE');
  });
  document.getElementById('detailYouGotBtn').addEventListener('click', () => {
    openAddTxModal('GOT');
  });

  function openAddTxModal(type) {
    document.getElementById('txPartyId').value = activePartyId;
    document.getElementById('txType').value = type;
    document.getElementById('txAmountInput').value = '';
    document.getElementById('txDescInput').value = '';
    document.getElementById('txModalTitle').textContent = type === 'GAVE' ? 'Record YOU GAVE (Udhar)' : 'Record YOU GOT (Payment)';
    showModal('addTxModal');
  }

  // Submit Transaction
  document.getElementById('submitTxBtn').addEventListener('click', async () => {
    const partyId = Number(document.getElementById('txPartyId').value);
    const type = document.getElementById('txType').value;
    const amount = parseFloat(document.getElementById('txAmountInput').value);
    const desc = document.getElementById('txDescInput').value.trim();
    const paymentMethod = document.getElementById('txPaymentMethod').value;

    if (!amount || isNaN(amount) || amount <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    const newTxId = await addItemToStore('transactions', {
      partyId,
      type,
      amount,
      description: desc,
      paymentMethod,
      dateMillis: Date.now()
    });

    // Also auto-log to cashbook if payment method is Cash
    if (paymentMethod === 'Cash') {
      await addItemToStore('cashbook', {
        type: type === 'GOT' ? 'IN' : 'OUT',
        amount,
        description: `Cash ${type === 'GOT' ? 'received from' : 'paid to'} party #${partyId}`,
        txId: newTxId,
        partyId: partyId,
        dateMillis: Date.now()
      });
    }

    hideModal('addTxModal');
    openPartyDetail(partyId);
    renderPartiesView();
  });

  // Cashbook buttons
  document.getElementById('openCashInBtn').addEventListener('click', () => {
    document.getElementById('cashEntryType').value = 'IN';
    document.getElementById('cashAmountInput').value = '';
    document.getElementById('cashDescInput').value = '';
    document.getElementById('cashModalTitle').textContent = 'Record Cash IN';
    showModal('cashModal');
  });

  document.getElementById('openCashOutBtn').addEventListener('click', () => {
    document.getElementById('cashEntryType').value = 'OUT';
    document.getElementById('cashAmountInput').value = '';
    document.getElementById('cashDescInput').value = '';
    document.getElementById('cashModalTitle').textContent = 'Record Cash OUT';
    showModal('cashModal');
  });

  document.getElementById('submitCashBtn').addEventListener('click', async () => {
    const type = document.getElementById('cashEntryType').value;
    const amount = parseFloat(document.getElementById('cashAmountInput').value);
    const desc = document.getElementById('cashDescInput').value.trim();

    if (!amount || isNaN(amount) || amount <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    await addItemToStore('cashbook', {
      type,
      amount,
      description: desc,
      dateMillis: Date.now()
    });

    hideModal('cashModal');
    renderCashbookView();
  });

  // Expense & Income buttons
  document.getElementById('openAddExpenseBtn').addEventListener('click', () => {
    document.getElementById('expIncType').value = 'EXPENSE';
    document.getElementById('expIncAmountInput').value = '';
    document.getElementById('expIncDescInput').value = '';
    document.getElementById('expIncModalTitle').textContent = 'Record Expense';
    showModal('expenseIncomeModal');
  });

  document.getElementById('openAddIncomeBtn').addEventListener('click', () => {
    document.getElementById('expIncType').value = 'INCOME';
    document.getElementById('expIncAmountInput').value = '';
    document.getElementById('expIncDescInput').value = '';
    document.getElementById('expIncModalTitle').textContent = 'Record Revenue / Income';
    showModal('expenseIncomeModal');
  });

  document.getElementById('submitExpIncBtn').addEventListener('click', async () => {
    const type = document.getElementById('expIncType').value;
    const amount = parseFloat(document.getElementById('expIncAmountInput').value);
    const category = document.getElementById('expIncCategory').value;
    const desc = document.getElementById('expIncDescInput').value.trim();

    if (!amount || isNaN(amount) || amount <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    if (type === 'EXPENSE') {
      await addItemToStore('expenses', { category, amount, description: desc, dateMillis: Date.now() });
    } else {
      await addItemToStore('incomes', { category, amount, description: desc, dateMillis: Date.now() });
    }

    hideModal('expenseIncomeModal');
    renderExpensesView();
  });

  // Settings Navigation
  const openSettingsBtn = document.getElementById('openSettingsBtn');
  if (openSettingsBtn) {
    const handleOpenSettings = (e) => {
      e.preventDefault();
      e.stopPropagation();
      switchView('settings');
    };
    openSettingsBtn.addEventListener('click', handleOpenSettings);
    openSettingsBtn.addEventListener('touchstart', handleOpenSettings, { passive: false });
  }

  const backFromSettingsBtn = document.getElementById('backFromSettingsBtn');
  if (backFromSettingsBtn) {
    backFromSettingsBtn.addEventListener('click', (e) => {
      e.preventDefault();
      switchView('parties');
    });
  }

  // Save Business Profile
  document.getElementById('saveSettingsBtn').addEventListener('click', async () => {
    const name = document.getElementById('settingBusinessName').value.trim() || 'My Business Khata';
    const subtitle = document.getElementById('settingBusinessSubtitle').value.trim();
    const phone = document.getElementById('settingBusinessPhone').value.trim();
    const address = document.getElementById('settingBusinessAddress').value.trim();
    const upi = document.getElementById('settingUpiId').value.trim();

    await Promise.all([
      setSetting('businessName', name),
      setSetting('businessSubtitle', subtitle),
      setSetting('businessPhone', phone),
      setSetting('businessAddress', address),
      setSetting('upiId', upi)
    ]);

    const headerBusinessEl = document.getElementById('headerBusinessName');
    if (headerBusinessEl) {
      headerBusinessEl.textContent = subtitle ? `${name} • ${subtitle}` : name;
    }

    showToast("Business profile saved");
  });

  // Language Select
  const langSelect = document.getElementById('settingLanguageSelect');
  if (langSelect) {
    langSelect.addEventListener('change', async (e) => {
      const newLang = e.target.value;
      await setSetting('language', newLang);
      applyLanguage(newLang);
      showToast("Language updated");
    });
  }

  // Theme Toggle
  const themeToggle = document.getElementById('settingThemeToggle');
  if (themeToggle) {
    themeToggle.addEventListener('change', async (e) => {
      const isDark = e.target.checked;
      const theme = isDark ? 'dark' : 'light';
      await setSetting('theme', theme);
      applyTheme(theme);
      showToast(isDark ? "Dark theme enabled" : "Light theme enabled");
    });
  }

  // PIN Lock Toggle & Save
  const pinToggle = document.getElementById('settingPinToggle');
  if (pinToggle) {
    pinToggle.addEventListener('change', async (e) => {
      const isEnabled = e.target.checked;
      const pinBox = document.getElementById('pinConfigBox');
      if (pinBox) pinBox.style.display = isEnabled ? 'block' : 'none';
      if (!isEnabled) {
        await setSetting('pinEnabled', false);
        showToast("PIN lock disabled");
      } else {
        const pinInput = document.getElementById('settingPinInput');
        if (pinInput) pinInput.focus();
      }
    });
  }

  const savePinBtn = document.getElementById('savePinBtn');
  if (savePinBtn) {
    savePinBtn.addEventListener('click', async () => {
      const pin = document.getElementById('settingPinInput').value.trim();
      if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
        alert("Please enter exactly 4 digits for your PIN.");
        return;
      }
      await setSetting('appPin', pin);
      await setSetting('pinEnabled', true);
      showToast("4-digit PIN saved successfully");
    });
  }

  // PIN Keypad Handlers
  document.querySelectorAll('.pin-key[data-digit]').forEach(key => {
    key.addEventListener('click', () => {
      handlePinDigit(key.getAttribute('data-digit'));
    });
  });

  const pinBackspaceBtn = document.getElementById('pinBackspaceBtn');
  if (pinBackspaceBtn) {
    pinBackspaceBtn.addEventListener('click', () => {
      if (enteredPinDigits.length > 0) {
        enteredPinDigits = enteredPinDigits.slice(0, -1);
        updatePinDots();
        const errEl = document.getElementById('pinErrorText');
        if (errEl) errEl.textContent = "";
      }
    });
  }

  const pinBypassBtn = document.getElementById('pinBypassBtn');
  if (pinBypassBtn) {
    pinBypassBtn.addEventListener('click', async () => {
      if (confirm("Reset PIN protection to access your ledger?")) {
        await setSetting('pinEnabled', false);
        hideModal('pinLockModal');
        showToast("PIN lock reset");
        const pt = document.getElementById('settingPinToggle');
        if (pt) pt.checked = false;
      }
    });
  }

  // Restore Data JSON
  const restoreDataBtn = document.getElementById('restoreDataBtn');
  const restoreFileInput = document.getElementById('restoreFileInput');
  if (restoreDataBtn && restoreFileInput) {
    restoreDataBtn.addEventListener('click', () => {
      restoreFileInput.click();
    });

    restoreFileInput.addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = async (event) => {
        try {
          const data = JSON.parse(event.target.result);
          if (!data.parties && !data.transactions && !data.settings) {
            alert("Invalid backup file format.");
            return;
          }

          requestConfirmation(
            "Restore from backup?",
            "This will restore all customers, transactions, and records from your backup file. Proceed?",
            async () => {
              const stores = ['parties', 'transactions', 'cashbook', 'expenses', 'incomes', 'settings'];
              for (const s of stores) {
                if (Array.isArray(data[s])) {
                  const tx = db.transaction(s, 'readwrite');
                  const store = tx.objectStore(s);
                  store.clear();
                  data[s].forEach(item => store.add(item));
                }
              }
              showToast("Data restored successfully");
              await loadInitialSettings();
              renderPartiesView();
              renderCashbookView();
              renderExpensesView();
              renderReportsView();
              switchView('parties');
            }
          );
        } catch (err) {
          console.error("Restore error:", err);
          alert("Failed to parse backup JSON file.");
        }
      };
      reader.readAsText(file);
    });
  }

  // Export CSV
  const exportCsvBtn = document.getElementById('exportCsvBtn');
  if (exportCsvBtn) {
    exportCsvBtn.addEventListener('click', async () => {
      const [parties, transactions] = await Promise.all([
        getAllFromStore('parties'),
        getAllFromStore('transactions')
      ]);

      if (transactions.length === 0) {
        alert("No transactions recorded yet to export.");
        return;
      }

      const partyMap = {};
      parties.forEach(p => partyMap[p.id] = p);

      let csv = "ID,Date,Party Name,Party Phone,Party Type,Type,Amount (INR),Payment Method,Description\n";
      transactions.forEach(t => {
        const p = partyMap[t.partyId] || { name: 'Unknown', phone: '', isSupplier: false };
        const dateStr = new Date(t.dateMillis).toISOString().slice(0, 10);
        const pType = p.isSupplier ? 'Supplier' : 'Customer';
        const typeLabel = t.type === 'GAVE' ? 'You Gave (Udhar)' : 'You Got (Payment)';
        const cleanDesc = (t.description || '').replace(/"/g, '""');
        csv += `"${t.id}","${dateStr}","${p.name}","${p.phone || ''}","${pType}","${typeLabel}","${t.amount}","${t.paymentMethod || 'Cash'}","${cleanDesc}"\n`;
      });

      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `HisabPro_Ledger_${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast("CSV export downloaded");
    });
  }

  // Export PDF / Print
  const exportPdfBtn = document.getElementById('exportPdfBtn');
  if (exportPdfBtn) {
    exportPdfBtn.addEventListener('click', () => {
      window.print();
    });
  }

  // Share App
  const shareAppBtn = document.getElementById('shareAppBtn');
  if (shareAppBtn) {
    shareAppBtn.addEventListener('click', async () => {
      const shareData = {
        title: 'HisabPro',
        text: 'HisabPro - Digital Khata & Udhar Ledger. Simple business, clear hisab.',
        url: window.location.href
      };

      if (navigator.share) {
        try {
          await navigator.share(shareData);
        } catch (e) {
          // cancelled
        }
      } else {
        const waUrl = `https://wa.me/?text=${encodeURIComponent(shareData.text + ' ' + shareData.url)}`;
        window.open(waUrl, '_blank');
      }
    });
  }

  // Reset All Ledger Data
  const resetAllDataBtn = document.getElementById('resetAllDataBtn');
  if (resetAllDataBtn) {
    resetAllDataBtn.addEventListener('click', () => {
      requestConfirmation(
        "Reset All Ledger Data?",
        "Are you sure you want to permanently delete all customers, suppliers, transactions, and cashbook records? Your business profile settings will be preserved.",
        async () => {
          const stores = ['parties', 'transactions', 'cashbook', 'expenses', 'incomes'];
          for (const s of stores) {
            const tx = db.transaction(s, 'readwrite');
            tx.objectStore(s).clear();
          }
          showToast("All ledger data reset");
          renderPartiesView();
          renderCashbookView();
          renderExpensesView();
          renderReportsView();
          switchView('parties');
        }
      );
    });
  }

  // Close modals on [data-close] or click outside
  document.querySelectorAll('[data-close]').forEach(btn => {
    btn.addEventListener('click', () => {
      const modalId = btn.getAttribute('data-close');
      hideModal(modalId);
    });
  });

  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        overlay.classList.remove('active');
      }
    });
  });

  // PWA Install Flow
  const triggerInstallBtn = document.getElementById('triggerPwaInstallBtn');
  triggerInstallBtn.addEventListener('click', handleInstallClick);

  // Export Data JSON
  document.getElementById('exportDataBtn').addEventListener('click', async () => {
    const [parties, transactions, cashbook, expenses, incomes] = await Promise.all([
      getAllFromStore('parties'),
      getAllFromStore('transactions'),
      getAllFromStore('cashbook'),
      getAllFromStore('expenses'),
      getAllFromStore('incomes')
    ]);

    const backup = {
      app: 'HisabPro',
      exportedAt: new Date().toISOString(),
      parties,
      transactions,
      cashbook,
      expenses,
      incomes
    };

    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `HisabPro_Backup_${new Date().toISOString().slice(0,10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  });

  // Confirmation Modal buttons
  const cancelBtn = document.getElementById('cancelDeleteBtn');
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
      pendingDeleteAction = null;
      hideModal('confirmDeleteModal');
    });
  }

  const executeBtn = document.getElementById('executeDeleteBtn');
  if (executeBtn) {
    executeBtn.addEventListener('click', async () => {
      if (isDeleting) return;
      if (typeof pendingDeleteAction === 'function') {
        isDeleting = true;
        executeBtn.disabled = true;
        executeBtn.textContent = 'Deleting...';
        try {
          await pendingDeleteAction();
        } catch (err) {
          console.error('Delete error:', err);
          alert('Failed to delete item.');
        } finally {
          isDeleting = false;
          executeBtn.disabled = false;
          executeBtn.textContent = 'Delete';
          pendingDeleteAction = null;
          hideModal('confirmDeleteModal');
        }
      }
    });
  }
}

// --- Delete Implementations ---

// 1. Delete Customer / Supplier
async function promptDeleteParty(partyId) {
  const parties = await getAllFromStore('parties');
  const party = parties.find(p => p.id === partyId);
  if (!party) return;

  const isSupplier = party.isSupplier;
  const title = isSupplier ? "Delete this supplier?" : "Delete this customer?";
  const message = isSupplier
    ? "All transaction history for this supplier will also be deleted."
    : "All transaction history for this customer will also be deleted.";

  requestConfirmation(title, message, async () => {
    // 1. Delete all transactions belonging to this party
    const allTxs = await getAllFromStore('transactions');
    const partyTxs = allTxs.filter(t => t.partyId === partyId);
    for (const t of partyTxs) {
      await deleteItemFromStore('transactions', t.id);
    }

    // 2. Remove any related auto-generated cashbook entries
    const cashEntries = await getAllFromStore('cashbook');
    const relatedCash = cashEntries.filter(c => 
      c.partyId === partyId || 
      (c.description && c.description.includes(`party #${partyId}`))
    );
    for (const c of relatedCash) {
      await deleteItemFromStore('cashbook', c.id);
    }

    // 3. Delete the party record
    await deleteItemFromStore('parties', partyId);

    // 4. Close detail modal if open
    hideModal('partyDetailModal');

    // 5. Notify & refresh UI immediately
    showToast("Deleted successfully");
    renderPartiesView();
    renderCashbookView();
    renderRemindersView();
    renderReportsView();
  });
}

// 2. Delete Individual Transaction
async function promptDeleteTransaction(txId) {
  const allTxs = await getAllFromStore('transactions');
  const tx = allTxs.find(t => t.id === txId);
  if (!tx) return;

  requestConfirmation(
    "Delete Transaction?",
    `Are you sure you want to delete this ${formatCurrency(tx.amount)} (${tx.type === 'GAVE' ? 'You Gave' : 'You Got'}) entry? All balances will be recalculated.`,
    async () => {
      // 1. Delete the transaction
      await deleteItemFromStore('transactions', txId);

      // 2. If it was cash payment, delete the related cashbook entry
      if (tx.paymentMethod === 'Cash') {
        const cashEntries = await getAllFromStore('cashbook');
        const match = cashEntries.find(c => 
          c.txId === txId || 
          (c.amount === tx.amount && c.partyId === tx.partyId) ||
          (c.amount === tx.amount && c.description && c.description.includes(`party #${tx.partyId}`))
        );
        if (match) {
          await deleteItemFromStore('cashbook', match.id);
        }
      }

      // 3. Refresh views and recalculate all balances immediately
      showToast("Deleted successfully");
      if (activePartyId) {
        await openPartyDetail(activePartyId);
      }
      renderPartiesView();
      renderCashbookView();
      renderRemindersView();
      renderReportsView();
    }
  );
}

// 3. Delete Expense or Income
async function promptDeleteExpenseIncome(itemId, isExpense) {
  const storeName = isExpense ? 'expenses' : 'incomes';
  const items = await getAllFromStore(storeName);
  const item = items.find(i => i.id === itemId);
  if (!item) return;

  const title = isExpense ? "Delete Expense?" : "Delete Income?";
  const msg = `Are you sure you want to delete this ${formatCurrency(item.amount)} (${escapeHtml(item.category)}) record?`;

  requestConfirmation(title, msg, async () => {
    // 1. Delete the expense/income
    await deleteItemFromStore(storeName, itemId);

    // 2. Remove any linked cashbook entry if exists
    const cashEntries = await getAllFromStore('cashbook');
    const cashMatch = cashEntries.find(c => 
      c.amount === item.amount &&
      c.description &&
      c.description.includes(item.category)
    );
    if (cashMatch) {
      await deleteItemFromStore('cashbook', cashMatch.id);
    }

    // 3. Refresh views immediately
    showToast("Deleted successfully");
    renderExpensesView();
    renderCashbookView();
    renderReportsView();
  });
}

// 4. Delete Cashbook Entry
async function promptDeleteCashbook(cashId) {
  const cashEntries = await getAllFromStore('cashbook');
  const entry = cashEntries.find(c => c.id === cashId);
  if (!entry) return;

  requestConfirmation(
    "Delete Cash Entry?",
    `Are you sure you want to delete this ${formatCurrency(entry.amount)} (${entry.type === 'IN' ? 'Cash IN' : 'Cash OUT'}) entry?`,
    async () => {
      await deleteItemFromStore('cashbook', cashId);
      showToast("Deleted successfully");
      renderCashbookView();
      renderReportsView();
    }
  );
}

// Expose delete functions to window for robust inline handler access
window.promptDeleteParty = promptDeleteParty;
window.promptDeleteTransaction = promptDeleteTransaction;
window.promptDeleteExpenseIncome = promptDeleteExpenseIncome;
window.promptDeleteCashbook = promptDeleteCashbook;

function switchView(viewName) {
  currentView = viewName;
  document.querySelectorAll('.app-view').forEach(v => v.style.display = 'none');
  const target = document.getElementById(`view-${viewName}`);
  if (target) target.style.display = 'block';

  // Highlight bottom nav
  document.querySelectorAll('.nav-item').forEach(n => {
    if (n.getAttribute('data-nav') === viewName) n.classList.add('active');
    else n.classList.remove('active');
  });

  if (viewName === 'parties') renderPartiesView();
  else if (viewName === 'cashbook') renderCashbookView();
  else if (viewName === 'expenses') renderExpensesView();
  else if (viewName === 'reminders') renderRemindersView();
  else if (viewName === 'reports') renderReportsView();
  else if (viewName === 'settings') loadSettingsValues();
}

async function loadInitialSettings() {
  const [name, subtitle, lang, theme] = await Promise.all([
    getSetting('businessName', 'My Business Khata'),
    getSetting('businessSubtitle', 'Digital Khata & Udhar Ledger'),
    getSetting('language', 'en'),
    getSetting('theme', 'light')
  ]);

  const headerBusinessEl = document.getElementById('headerBusinessName');
  if (headerBusinessEl) {
    headerBusinessEl.textContent = subtitle ? `${name} • ${subtitle}` : name;
  }

  applyTheme(theme);
  applyLanguage(lang);
}

async function loadSettingsValues() {
  const [name, subtitle, phone, address, upi, lang, theme, pinEnabled, appPin] = await Promise.all([
    getSetting('businessName', 'My Business Khata'),
    getSetting('businessSubtitle', 'Digital Khata & Udhar Ledger'),
    getSetting('businessPhone', ''),
    getSetting('businessAddress', ''),
    getSetting('upiId', ''),
    getSetting('language', 'en'),
    getSetting('theme', 'light'),
    getSetting('pinEnabled', false),
    getSetting('appPin', '')
  ]);

  const nameEl = document.getElementById('settingBusinessName');
  if (nameEl) nameEl.value = name;
  const subEl = document.getElementById('settingBusinessSubtitle');
  if (subEl) subEl.value = subtitle;
  const phoneEl = document.getElementById('settingBusinessPhone');
  if (phoneEl) phoneEl.value = phone;
  const addrEl = document.getElementById('settingBusinessAddress');
  if (addrEl) addrEl.value = address;
  const upiEl = document.getElementById('settingUpiId');
  if (upiEl) upiEl.value = upi;

  const langSelect = document.getElementById('settingLanguageSelect');
  if (langSelect) langSelect.value = lang;

  const themeToggle = document.getElementById('settingThemeToggle');
  if (themeToggle) themeToggle.checked = (theme === 'dark');

  const pinToggle = document.getElementById('settingPinToggle');
  const pinBox = document.getElementById('pinConfigBox');
  const pinInput = document.getElementById('settingPinInput');
  if (pinToggle) pinToggle.checked = pinEnabled;
  if (pinBox) pinBox.style.display = pinEnabled ? 'block' : 'none';
  if (pinInput) pinInput.value = appPin;
}

// --- PWA Installation Logic ---
function isIosSafari() {
  const ua = window.navigator.userAgent;
  const isIos = /iPhone|iPad|iPod/.test(ua);
  const isWebkit = /WebKit/.test(ua);
  const isChrome = /CriOS/.test(ua);
  const isFxiOS = /FxiOS/.test(ua);
  return isIos && isWebkit && !isChrome && !isFxiOS;
}

function isStandalone() {
  return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

function handleInstallClick() {
  if (isStandalone()) {
    alert('HisabPro is already installed and running in standalone app mode!');
    return;
  }

  showModal('pwaInstallModal');

  if (isIosSafari()) {
    // Show iOS Safari Step-by-Step Instructions
    document.getElementById('iosInstallInstructions').style.display = 'block';
    document.getElementById('androidInstallInstructions').style.display = 'none';
  } else {
    // Show Android Chrome native install trigger
    document.getElementById('iosInstallInstructions').style.display = 'none';
    document.getElementById('androidInstallInstructions').style.display = 'block';

    const nativeBtn = document.getElementById('nativePwaInstallBtn');
    nativeBtn.onclick = async () => {
      if (deferredInstallPrompt) {
        deferredInstallPrompt.prompt();
        const { outcome } = await deferredInstallPrompt.userChoice;
        if (outcome === 'accepted') {
          hideModal('pwaInstallModal');
        }
        deferredInstallPrompt = null;
      } else {
        alert('To install, tap the three dots in your browser menu and select "Install app" or "Add to Home Screen".');
      }
    };
  }
}

// Capture Chrome beforeinstallprompt
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredInstallPrompt = e;
});

// Register Service Worker
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then(reg => console.log('HisabPro Service Worker registered:', reg.scope))
      .catch(err => console.error('Service Worker registration failed:', err));
  });
}

// --- Application Startup ---
window.addEventListener('DOMContentLoaded', async () => {
  await openDatabase();
  await loadInitialSettings();
  setupEventListeners();
  renderPartiesView();
  checkAppPinLock();
});
