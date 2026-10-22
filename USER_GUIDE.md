# Arkon – User Guide

This guide explains every part of Arkon (Arkon – Business Management) for the
people who use it every day: what each option does, when to use it, and what
happens behind the scenes. For installation and server setup, see `README.md`.

**Contents**

1. [Getting around](#1-getting-around)
2. [Setting up before first use](#2-setting-up-before-first-use)
3. [Purchases](#3-purchases)
4. [Sales](#4-sales)
5. [Offers](#5-offers)
6. [Payments and debts](#6-payments-and-debts)
7. [Returns and stock adjustments](#7-returns-and-stock-adjustments)
8. [How stock is counted](#8-how-stock-is-counted)
8b. [Shelves and pick lists](#8b-shelves-and-pick-lists)
8c. [Cash & Bank](#8c-cash--bank)
8d. [Cost centres](#8d-cost-centres)
8e. [Payroll, Fixed assets, Production, Tax due](#8e-payroll-fixed-assets-production-tax-due)
8f. [Accounting](#8f-accounting)
8g. [Statements, stock count, reminders, period lock, app](#8g-statements-stock-count-reminders-period-lock-app)
8h. [Budgets, cash-flow forecast, two-step login, price lists](#8h-budgets-cash-flow-forecast-two-step-login-price-lists)
8i. [Multi-currency (EUR, USD)](#8i-multi-currency-eur-usd)
8j. [Quick sale (POS)](#8j-quick-sale-pos)
8k. [Shops and warehouses](#8k-shops-and-warehouses)
8l. [Batches, expiry dates and serial numbers](#8l-batches-expiry-dates-and-serial-numbers)
9. [Invoices](#9-invoices)
10. [Reports](#10-reports)
11. [Statistics](#11-statistics)
12. [Administration](#12-administration)
13. [Common situations](#13-common-situations)

---

## 1. Getting around

### Logging in

Enter your username and password on the login page. The two flag buttons
under the form switch the language between **Shqip** and **English** before
you log in.

### The menu bar

The top bar has a few **sections**, in the order of daily work. Click a
section to open it; its pages then appear as **buttons at the top of the
page**, under the top bar, and the page you're on is marked in amber.

| Section | Pages (tabs) |
|---|---|
| Paneli (Dashboard) | your widgets |
| Sales | new sale · quick sale (POS) · sales · new offer · offers · returns · sale and offer invoices · unpaid by clients · aged receivables · client statement · reminders |
| Purchases | new purchase · purchases · returns · purchase orders · for supply · purchase invoices · unpaid to merchants · aged payables · supplier statement |
| Stock | products · warehouse report · stock adjustments · stock count · stock by location · transfers · locations · expiry · serial numbers · production · bills of materials · shelves · classes · price lists |
| Cash & Bank | overview · movements · money in / out · transfer · reconcile · cash flow · analytics · accounts · exchange rates · exchange differences |
| Accounting | journal · general ledger · trial balance · statements · **official statements** · VAT books · chart of accounts · tax due · fixed assets · budget · budget vs actual · forecast · cost centre report |
| Payroll | payroll runs · employees · settings |
| Reports | statistics · sales · purchases · products |
| Administration | clients · merchants · companies · users · settings · email · backup · audit log · cost centres |

- **Who sees what:** each user only sees the sections and tabs they have
  access to (see *Roles and access*).
- **Optional modules:** their tabs only appear when switched on.

### Search

The **🔍** button in the top bar, or **Ctrl+K** (or **/**) on any page,
opens a search over every page in **your** menu:

- **Search by name:** type part of a name, e.g. *aged*, *kurs*, *transfer*,
  *TVSH*. Accents don't matter (*fature* finds *faturë*). Each result shows
  where it is in the menu.
- **Open an order:** type an order number, e.g. *12*, to get **Open sale /
  purchase / offer 00000012**.
- **Keys:** **↑ ↓** to choose, **Enter** to open, **Esc** to close, or
  click a result.

It only shows pages you have access to, and only modules that are switched
on.

### The account menu (top right)

Click your initials and username to open it:

- **Profile card:** your full name, username and role (Administrator, User
  or Accountant).
- **Last login:** when you last logged in before this session. If you don't
  recognise the time, tell an administrator.
- **Language:** the 🇦🇱 / 🇬🇧 buttons. The choice is remembered on that
  computer.
- **Change password:** you need your current password. After the change you're
  logged out and log in again with the new one.
- **Log out.**

### Light and dark theme

The round sun button left of your username switches to the dark theme; it then
shows a moon, which switches back. The choice is remembered in that browser.
Printed pages always use a white background.

### Roles and access

There are three roles:

- **Administrator:** everything, including Administration (users,
  settings, backup, audit log, registers).
- **User:** purchases, sales, offers and their invoices (their own orders).
- **Accountant:** **view only**. Sees everyone's orders, reports,
  statistics, unpaid amounts, Cash & Bank, tax due, payroll, assets,
  production and cost centres, and can export and print, but can't change
  anything.

**Access per user** (Administration → Users → a user → **Access**): every
area can be set to **Role default**, **No access**, **View only** or **Full
access**. For example, a salesperson can also get **View only** on Cash &
Bank, or an accountant **Full access** to Payroll.

- **Enforced on the server:** without View access, a page answers "no
  access"; without Full access, nothing can be saved.
- **Modules:** a module switched off in Settings stays hidden for everyone.
- **Administrators** always have full access.

### Home

After logging in you land on your **Dashboard** (*Paneli*), described next.
The logo at the top left always brings you back to it.

### Dashboard (Paneli)

The first page after logging in is **your own dashboard**. Each user builds
theirs from a list of widgets:

| Widget | Shows |
|---|---|
| Quick actions | New sale / purchase / offer buttons |
| Sales today, Sales this month | Value with VAT; this month also compared with the same days of last month (▲/▼ %) |
| Sales, last 30 days | Bar chart per day |
| Top products this month | The five best-selling products by value |
| Latest sales | The six most recent sales |
| Unpaid by clients / to merchants | What's still owed, red when something is overdue |
| Open offers, Returns this month | Counts and values |
| To restock | Products below their quantity limit |
| Open purchase orders | Purchase orders not yet received |
| Cash and bank balances | The balance of each account (Cash & Bank module) |
| Taxes to pay | Open VAT and payroll taxes with due dates (Tax due module) |

**Customize** (top right of the dashboard):
- **Add** widgets from **Available**, **remove** them, and move them **▲ / ▼**.
- **Save** keeps your layout. **Reset to the default** goes back to your
  role's standard dashboard.
- **Access:** you only see widgets for areas you have access to, and
  figures follow it too. Users see their own sales; administrators and
  accountants see everyone's.

### Purchase type and supplier invoice number

- **On every saved purchase:** a bar with:
  - **Purchase type:** goods from local suppliers (default), imported
    goods, investment from a local supplier, imported investment, or from
    local farmers;
  - **Supplier invoice no.:** the number on the supplier's invoice.

  Click **Save**. The type decides the columns in the tax office's purchase
  book.
- **Default type per supplier** (Administration → Merchants → a merchant):
  e.g. "Imported goods" for a foreign supplier. New purchases from that
  supplier start with it.

### Aged receivables and payables

**Reports → Aged receivables** (what clients owe) and **Aged payables**
(what you owe suppliers): per client or supplier, the open amount **not yet
due**, and **1–30, 31–60, 61–90 and over 90 days** overdue, with totals.
- **How it's counted:** from each invoice's due date; invoices without a due
  date count as not yet due.
- **Export to Excel** and **Print**.

### Searching lists

On lists with a **Search** box, results update as you type. **From / To** dates
and **Rows** apply as soon as you change them, and the **Search** button also
works. The address bar keeps your search, so refreshing the page keeps it.

### Choosing columns

Every grid has a **☰ Columns** button above its top-right corner:

- **Untick** a column to hide it; tick it to show it again.
- **Show all** brings every column back.
- **Remembered:** the choice is saved per page in that browser.
- **Totals stay aligned:** the totals row adjusts to the visible columns.
- **Excel exports** always contain every column.

---

## 2. Setting up before first use

Set things up in this order: each step uses the one before it.

1. **Settings** (Administration → Settings)
   - **Money and invoices:**
     - **Currency:** e.g. ALL.
     - **VAT %:** e.g. 20.
     - **Invoice colour:** the accent colour on invoices.
   - **Invoice logo:** upload your logo (PNG, JPG or GIF).
   - **Payment terms and offers:**
     - **Payment due after (days):** the default due date for sales and
       purchases (default 30).
     - **Offers valid for (days):** how long a new offer stays open (default 15).
2. **Your company** (Administration → Companies). It's the seller on sales and
   offers and the buyer on purchases. Fill in name, TIN (NIPT), address, email
   and phone, since they print on invoices.
3. **Merchants:** the suppliers you buy from.
4. **Clients:** the customers you sell to.
5. **Products** (Administration → Products):

   | Field | Meaning |
   |---|---|
   | Name | Must be unique. Stock is counted by this name (see [section 8](#8-how-stock-is-counted)) |
   | Barcode / product code | Optional, unique. Your own product code (e.g. KB-325) or the barcode. Shown on order lines, shelves and pick lists; scanning or typing it on an order page picks the product |
   | Unit | pcs, kg, m, box… |
   | Buy price | The usual purchase price. Used as the default on purchases |
   | Sale price | The price on sales and offers, before discount |
   | Quantity limit | The **minimum stock to keep**. 0 = no limit (see below) |
   | Discount class | The group used for client discounts (see Product classes) |

   **Quantity limit** example: the limit is 5 and 7 are in stock. Selling 3 would
   leave 4, which is below 5, so the order page warns you. You can still sell;
   it's only a warning. The product also appears in the warehouse **For supply**
   report and in **To restock** on Home.

6. **Product classes and discounts** (Administration → Product classes and
   discounts)
   - **Class:** a group of products, e.g. "Cables" or "Lighting". A product
     belongs to one class.
   - **Client discount:** a client gets a percentage off every product in a
     class, e.g. *Beni Shehu, 5 % off Cables*. Sales and offers to that client
     apply it automatically.

7. **Users** (Administration → Users): create an account for each person, with
   the role **User** or **Administrator**.

---

## 3. Purchases

A purchase records stock **coming in** from a merchant.

### Creating a purchase

1. **Open it:** Purchases → **New purchase**. The page says "New purchase"
   without a number. It gets the next number (00000001, 00000002…) when the
   first product is saved, so abandoned pages don't leave gaps.
2. **Merchant and company:** choose the **Merchant** (supplier) and your
   **Company**. Their details appear underneath.
3. **Add products one at a time:**
   - **Scan or type code:** scan a barcode (or type the code and press Enter)
     to pick the product. Or choose it from the **Product** list.
   - **Quantity.**
   - **Put on shelf** (optional): the shelf where you'll store this delivery.
     The quantity is added to that shelf.
   - **Price paid per unit:** leave it empty to use the product's buy price.
     Fill it in when this delivery cost a different price; this is what makes
     profit figures accurate (see [Statistics](#11-statistics)).
   - The price row shows price, unit and the values without VAT, VAT and with
     VAT as you type.
   - Click **Add product**.
4. **Done:** after the first product, the merchant and company are fixed for
   this order.

Purchases have no stock limit: you can always buy.

### After saving

- **Create invoice:** makes the PDF (administrators).
- **Payments:** record what you've paid the merchant (see
  [section 6](#6-payments-and-debts)).
- **Remove:** takes a wrong line off the order. To change a quantity, remove
  the line and add it again.

---

## 4. Sales

A sale records stock **going out** to a client.

### Creating a sale

1. **Open it:** Sales → **New sale**. Choose your **Company** and the **Client**.
2. **Choose a product:** scan or choose it. Under **Quantity** you see how many
   are in stock (green), or **Out of stock** (red).
   **Take from shelf** (only when shelves are switched on). This list shows
   each shelf holding the product with its quantity, e.g. *A1 (2 cope)*,
   *A2 (3 cope)*, plus *Not on a shelf* for stock that isn't on any shelf:
   - **One place:** it's selected automatically.
   - **Several places:** choose where to take it from.
   - **More than one shelf needed:** add the product twice, e.g. 2 from A1
     and 3 from A2.
3. **Read the price row:**

   | Column | Meaning |
   |---|---|
   | Price | The product's sale price |
   | Unit | pcs, kg… |
   | Discount | The client's discount for this product's class (0 % if none) |
   | Unit price | Price after the discount |
   | Value (no VAT) / VAT / Value (with VAT) | For the quantity typed |

4. **Messages under the price row:**
   - **"Not enough stock…"** (red): you typed more than is in stock. **Add
     product** is disabled until you lower the quantity. The server checks this
     again, so it can't be bypassed.
   - **"After this, only … will remain in stock, below the quantity limit"**
     (amber): only a warning. You can still add the line.
5. **Add product.**

### Cost of each sale line

When a sale line is saved, Arkon also saves the product's **average purchase
cost** at that moment. You don't see this on the order, but Statistics uses it
for profit. See [section 11](#11-statistics).

### After saving

**Payments**, **Create invoice** and **Remove** work as described for
purchases. Removing a sale line puts its quantity back on the shelf it came
from.

**Pick list:** the sale page has **Pick list** (a printable page) and **Pick
list PDF**. Give it to the person collecting the goods (see
[Shelves and pick lists](#8b-shelves-and-pick-lists)).

### Returns (credit notes and return notes)

Returns are recorded **on the order** they belong to, so everything is
corrected at once. They work the same on both sides:

- **Sales:** goods a client brings back. Stock goes **up**, and a **credit
  note** is created.
- **Purchases:** goods you send back to the merchant (broken, wrong item…).
  Stock goes **down**, and a **return note** is created. You can only return
  what's still in stock.

Steps:

1. **Enter the quantities:** open the sale or purchase, scroll to
   **Returns** and enter how many of each item.
2. **Choose a reason:**
   - damaged or broken;
   - defective, not working;
   - wrong item;
   - no longer needed;
   - quality not as expected;
   - other.

   Add a note if useful. Reasons feed the return statistics.
3. **Shelf** (only if shelves are on): where the goods go back to, or are
   taken from. By default it's the shelf they came from.
4. **Record:** click **Record return**.

What happens:

- **The order:** gets minus lines marked **Return**, so its total goes down.
- **Every total follows:** reports, statistics, profit, the Home figures,
  unpaid amounts and the list totals.
- **The PDF:** the credit note or return note (e.g. 00000012-1) is created,
  linked on the order and listed under Invoices.
- **Refunds:**
  - **Sales:** if the client had already paid, the sale shows **Refund
    due**. Record the money with **Record refund** in Payments.
  - **Purchases:** if you had already paid the merchant, the purchase shows
    **Refund due**. Record the money with **Record refund received** when
    the merchant pays it back.

**Returns lists:** all returns are listed under **Sales → Sales returns**
and **Purchases → Returns to merchants**. Each shows the date, number,
client or merchant, products, units, value, reason, note and PDF. You can
search them and filter by date.

To undo a return, remove its **Return** lines from the order. A line that
was partly returned can't be removed until its return lines are removed.

The stock adjustments in section 7 are still there for stock changes that
don't belong to a sale or purchase in Arkon.

---

## 5. Offers

An offer is a price quotation. It **does not change stock** and is not a sale
until you turn it into one.

### Creating an offer

1. **Open it:** Offers → **New offer**. Choose your **Company**.
2. **Choose who it's for:**
   - **An existing client:** pick from the list. Their class discounts apply
     automatically.
   - **Someone new:** tick **New client (not registered yet)**, then:
     - type their name and fill in TIN, phone, address and email (optional,
       printed on the offer);
     - optionally enter a **Discount %** by hand; it applies to every line;
     - if the name you type is already a client, a hint tells you to choose it
       from the list instead.
3. **Add products,** as on a sale. The stock check also applies to offers, so
   you can't offer more than you have.

### Offer status

Each saved offer has an **Offer status** panel:

| Status | Meaning |
|---|---|
| Open | Waiting for the client's answer |
| Accepted | The client said yes (set by you) |
| Rejected | The client said no (set by you) |
| Expired | Shown automatically when an **Open** offer passes its **Valid until** date |
| Converted | The offer was turned into a sale; a link to the sale is shown |

Change **Status** or **Valid until** and click **Save**.

### Turn into sale

**Turn into sale** creates a new sale from the offer in one step:

- **Lines and prices:** every line is copied with the **prices that were
  offered**, even if prices changed since.
- **Stock check first:** stock is checked for every product. If anything is
  short, nothing is created and a message lists what's missing.
- **New clients:** if the offer was for a new client, they're **registered as
  a client automatically**, using the details from the offer. This needs an
  administrator.
- **Afterwards:** the offer becomes **Converted** with a link to the sale, the
  sale opens, and stock is reduced.

Statistics shows how many offers turn into sales (see
[section 11](#11-statistics)).

---

## 6. Payments and debts

Every **sale** and **purchase** tracks what's been paid. Offers don't, because
nothing is owed on an offer.

### The Payments panel (on the order page)

| Field | Meaning |
|---|---|
| Total | The order's value with VAT |
| Paid | Sum of the payments recorded |
| Still to pay | Total minus paid |
| Due date | When payment is due. Defaults to the order date + payment terms (Settings). Change it and click **Save** |

**Statuses:**

| Status | When |
|---|---|
| **Unpaid** | Nothing paid yet, not past the due date |
| **Partly paid** | Something paid, not past the due date |
| **Overdue** | Not fully paid and past the due date (shows how many days) |
| **Paid** | Fully paid |

### Recording a payment

1. **Fill in the form** under the totals:
   - **Date:** the date the money was received or paid.
   - **Amount:** defaults to the full amount still to pay; change it for a
     partial payment.
   - **Method:** Cash, Bank transfer, Card or Other.
   - **Note:** optional, e.g. a bank reference.
2. **Save it:** click **Record payment received** (sales) or **Record payment
   made** (purchases).

You can't record more than what's still owed. Several partial payments are
fine; they add up.

**Deleting a payment** (administrators): use **Delete** next to it in the
payment list. The order's status updates immediately.

### Where payment status appears

- **Lists:** Sales and Purchases have a **Payment** column.
- **Invoices:** the PDF shows **Amount due**, **Balance due** (after partial
  payments) or **Paid in full**.
- **Home:** the **Unpaid by clients** figure.

### Debts pages

**Reports → Unpaid by clients** (money owed to you) and **Reports → Unpaid to
merchants** (money you owe).

- **Figures:** total still to pay, and total overdue.
- **Per client / per merchant:** how many open orders, how much is still owed,
  how much of it is overdue, and the oldest due date. Sorted by amount owed.
- **Orders:** every unpaid or partly paid order, most overdue first. Click a
  row to open the order at its Payments panel.
- **Export to Excel.**

**Orders from before this feature** have no due date stored, so Arkon assumes
order date + payment terms. They may show as **Overdue** at first. Record their
payments, or adjust the terms in Settings.

---

## 7. Returns and stock adjustments

**Where:** Administration → **Returns and stock adjustments** (also linked from
the warehouse report).

**What it's for:** changing stock **without a purchase or sale**. Use it
whenever the physical stock changes for any other reason, or when the
recorded stock is simply wrong.

### The reasons

Choose the reason that matches what happened. The reason decides whether stock
goes **up** or **down**; you always type a positive quantity.

| Reason | Stock | Use it when | Example |
|---|---|---|---|
| **Returned by a client** | **+** | A client brings goods back | A client returns 2 faulty switches from sale 00000012 |
| **Returned to a merchant** | **−** | You send goods back to a supplier | You return 10 m of damaged cable to the merchant |
| **Damaged** | **−** | Goods broke or became unsellable in your warehouse | A box of bulbs was dropped; 6 broke |
| **Lost or stolen** | **−** | Goods are missing | 1 drill disappeared from the shop |
| **Stock count: more than recorded** | **+** | A physical count finds more than Arkon shows | Arkon says 40, the shelf has 42: add 2 |
| **Stock count: less than recorded** | **−** | A physical count finds fewer than Arkon shows | Arkon says 40, the shelf has 37: remove 3 |
| **Other: add to stock** | **+** | Any other increase | A free sample from a supplier |
| **Other: remove from stock** | **−** | Any other decrease | Goods taken for your own use |

### Recording an adjustment

1. **Choose the product:** pick it from the list (its unit is shown).
2. **Choose the reason:** the list is split into **Adds to stock** and
   **Removes from stock**.
3. **Enter the quantity:** always positive.
4. **Shelf** (optional): for removals, the shelf the goods come off; for
   additions, the shelf they go onto. Use it whenever the product is on
   shelves, so the shelves stay accurate.
5. **Reference** (optional): e.g. the sale or purchase number it relates to.
   Recommended for returns.
6. **Note** (optional): what happened.
7. **Save adjustment.**

**Rules:**

- **Never below zero:** removing more than is in stock is refused, with a
  message saying how many are actually in stock.
- **Signs in the list:** green **+** for additions, red **−** for removals.
- **Search:** live, by product, reference or note.

### Undo

**Undo** next to an adjustment cancels it, and stock changes back:

- Undoing a **removal** adds the stock back.
- Undoing an **addition** removes it again. This is refused if that stock has
  meanwhile been sold, because stock would go negative.

Every adjustment and undo is recorded in the audit log.

### Returns and money

A return changes **stock only**; it doesn't change any order or payment. If the
client gets money back:

- **Before they paid:** record a smaller payment, so the invoice stays partly
  open. Then change the due date or settle it as agreed.
- **After they paid:** give the refund outside Arkon, and write it in the
  adjustment's **Note** for reference.

The original sale stays unchanged, so its invoice and sales figures still show
the original sale.

### Worked example: a client returns goods

1. **The sale:** on sale 00000012, Beni Shehu bought 12 automatic switches.
2. **The return:** he returns 2 faulty ones.
3. **In Arkon:** add an adjustment with product *Automatic switch 16A*, reason
   **Returned by a client**, quantity **2**, reference **00000012**, note
   *"faulty, swapped next week"*.
4. **Result:** stock goes up by 2, and the warehouse report shows **+2** in the
   **Adjusted** column.
5. **If the switches can't be resold:** add a second adjustment with reason
   **Damaged**, quantity **2**. Stock goes back down, and the record shows
   exactly what happened.

### Worked example: a stock count

1. **Count the warehouse:** compare each product's real quantity with
   **Administration → Products** (the "In stock" column) or the warehouse
   report.
2. **Where the shelf has more:** add **Stock count: more than recorded** with
   the difference.
3. **Where the shelf has less:** add **Stock count: less than recorded** with
   the difference.
4. **Reference:** use something like *"Count 31/12/2026"* on all of them, so
   you can find them together later with the search box.

---

## 8. How stock is counted

For each product:

```
In stock = everything purchased
         − everything sold
         + adjustments that add (returns from clients, stock count +, other +)
         − adjustments that remove (returns to merchants, damage, loss, stock count −, other −)
```

- **Offers never change stock.**
- **The same figure everywhere:** this number is used by the sale and offer
  stock check, the **In stock** column in Products, the warehouse report and
  **To restock** on Home.
- **Counted by product name:** if you rename a product, its stock count starts
  again from zero, because past orders keep the old name. Rename only products
  without stock, or record a stock adjustment after renaming.

---

## 8b. Shelves and pick lists

**Where:** Administration → **Shelves**.

**Shelves are optional and switched off by default.** Turn them on in
**Administration → Settings → Shelves** (**Use shelves**). When they're off:

- **Hidden:** shelf fields, shelf columns and the Shelves menu item
  disappear.
- **Never limiting:** shelves no longer affect purchases, sales or
  adjustments.
- **Kept:** your shelf data stays, so you can switch them back on any time.


Shelves record **where** your stock physically is, so whoever collects a sale
knows exactly where to go. They don't change **how much** you have; that still
comes from purchases, sales and adjustments (section 8).

### How it works

- **Spread over shelves:** a product's stock can be on several shelves, e.g.
  Cable 3x2.5 → **A1: 20 m, B2: 30 m**.
- **Not on a shelf:** stock that isn't on any shelf. All your existing stock
  starts here, so everything keeps working before you set up shelves.

  ```
  Not on a shelf = total stock − everything on shelves
  ```

- **Always consistent:** shelves can never hold more than the total. You can
  only take from a shelf what's on it.

### Setting up

1. **Add your shelves:** a **code** (A1, A2, B1…, unique) and an optional
   **description** (e.g. "Back room, top").
2. **Place existing stock:** the **Not on a shelf yet** box lists products with
   unplaced stock. Use **Place or move stock**: choose the product and
   quantity, **From: Not on a shelf**, **To:** the shelf, then **Move**.
3. **Check:** each shelf card lists its products, codes and quantities. The
   **Shelves** column in Administration → Products shows e.g. *A1: 2, A2: 3*
   for each product.

### Place or move stock

| From | To | Result |
|---|---|---|
| Not on a shelf | A shelf | Places stock on the shelf |
| A shelf | Another shelf | Moves it between shelves |
| A shelf | Not on a shelf | Takes it off the shelf (it stays in stock) |

You can't move more than the source holds. Every move is recorded in the
audit log.

### Purchases, sales and adjustments

| Action | Shelf effect |
|---|---|
| Purchase with **Put on shelf** | Adds the quantity to that shelf |
| Purchase without a shelf | Stock arrives as *not on a shelf* |
| Sale with **Take from shelf** | Removes the quantity from that shelf (refused if the shelf has less) |
| Sale from *Not on a shelf* | Allowed for stock that isn't on a shelf; otherwise Arkon asks you to choose the shelf |
| Removing a sale line or deleting a sale | Puts the quantity back on its shelf |
| Removing a purchase line or deleting a purchase | Takes the quantity back off its shelf |
| Stock adjustment with a shelf | Removals come off it, additions go onto it; **Undo** reverses it |
| Offer turned into a sale | Arkon picks: stock not on a shelf first, otherwise the one shelf with enough. If the stock is split over several shelves, it asks you to create the sale by hand |
| Deleting a shelf | Its stock isn't lost; it becomes *not on a shelf* |

### Pick lists

On a saved sale, click **Pick list** (opens a page to print) or **Pick list
PDF**. It shows:

- the sale number, client, date, your company, and who prepared it;
- each line with **code**, **product**, **quantity** and the **shelf**, shown
  large on an amber background so it's easy to read in the warehouse;
- a **tick box** per line;
- signature lines for **Picked by**, **Checked by** and **Date and time**.

A product taken from two shelves appears as two lines.

### Worked example

1. **Receive goods:** a purchase of 50 m cable is put on shelf **A1**. A1 now
   holds 50 m.
2. **Rearrange:** you move 30 m from **A1** to **B2**. Now A1 holds 20 m and
   B2 holds 30 m.
3. **Sell from one shelf:** a sale of 25 m. Choose **B2 (30 m)**; B2 then holds
   5 m.
4. **Sell from two shelves:** a client wants 22 m. Add **20 m from A1** and
   **2 m from B2** as two lines.
5. **Collect:** print the **Pick list**. The warehouse sees A1 → 20 m and
   B2 → 2 m, ticks both off, and signs.

---

## 8c. Cash & Bank

**Where:** the **Arka & Banka** (Cash & Bank) tab, for administrators.
It's an **optional module, off by default**: switch it on in **Settings →
Modules**. While it's off, the tab, pages and the payment **Account** field
are hidden, but payments are still recorded in the background, so switching
it on later shows the full history.

It shows how much money you have in each cash register and bank account,
and every entry and exit.

### Accounts

- **Defaults:** two accounts exist from the start, **Arka** (cash) and
  **Banka** (bank).
- **Accounts page:** add more, e.g. a second bank, with the bank name and
  IBAN. Each account has:
  - an **opening balance** and **opening date** (what was in it when you
    started using Arkon);
  - **Active** (inactive accounts are hidden from forms);
  - **Default** (the account payments go to automatically).
- **Deleting:** an account with movements can't be deleted; make it
  inactive instead.

### Payments go in automatically

When you record a payment or refund on a sale or purchase, it becomes a
movement:

| Payment | Account | Direction |
|---|---|---|
| Client pays a sale | Cash → Arka, Bank transfer or Card → Banka | In |
| You pay a purchase | Same | Out |
| Refund to a client | Same | Out |
| Refund from a merchant | Same | In |

- **Choosing the account:** the payment form has an **Account** field that
  follows the method. You can choose a different account.
- **Deleting:** deleting a payment (or its order) removes the movement too.
- **Older payments:** those recorded before this feature were carried over
  (cash to Arka, others to Banka).

### Other money in and out

Use **+ Money in** / **− Money out** for anything that isn't a sale or
purchase payment:

- **In:** capital, loan received, other income.
- **Out:**
  - rent, salaries, utilities, transport and fuel;
  - taxes, bank fees;
  - owner withdrawal, loan repayment;
  - other expenses.

Enter the account, date, category, amount, a description and an optional
reference (e.g. a receipt number).

### Transfers

**Transfer** moves money between your own accounts, e.g. depositing the day's
cash into the bank: Arka goes down, Banka goes up. Transfers are not
income or expense, so the cash flow report leaves them out.

### Reconcile (barazo gjendjen)

1. **Count:** count the cash in the till, or look at the bank statement.
2. **Enter the real balance:** in **Reconcile**, choose the account, the
   date and the balance you counted (Arkon shows its own figure next to each
   account).
3. **Correction:** if they differ, Arkon records the difference as a
   **Correction (+/−)**, so its balance matches reality. Investigate big
   differences.

### Overview, movements and cash flow

- **Overview:**
  - the balance of cash, bank and the total;
  - one card per account;
  - this month's in, out and net;
  - the latest movements.

  Click an account card to see its movements.
- **Movements:** every entry and exit, filtered by account, category, dates
  and search. With **one account** chosen, it shows the **opening balance**
  and the **balance after each movement**, like a bank statement. It has
  totals and **Export to Excel**. Hand-entered movements and transfers can
  be deleted here; payments are deleted on their order.
- **Cash flow:** money in and out by category for any period, the net
  result, and a chart per day (or month for long periods).
- **Analytics (Analitika):** comparisons.
  - **A month vs the previous month**, or vs **the same month last year**.
    For the current month it compares the same number of days, e.g. 1–20
    October vs 1–20 September.
  - **This year so far vs the same days last year.**
  - **Change %:** in, out and net each show the change, in green when it's
    good (more in, less out, higher net) and red when it's not.
  - **Per category:** each category's amount in both periods and the
    difference.
  - **Charts for the last 12 months:**
    - money in and out next to the same months a year earlier;
    - the net result per month (with last year's line);
    - the total balance at each month end.

---

## 8d. Cost centres

**Optional module, off by default.** Switch it on in **Settings → Modules**.

A **cost centre** (*qendra e kostos*) is anything you want to see the result
of separately: a branch, a project, a department, a product line or a big
client contract.

### Setting up

**Administration → Cost centres:** add each one with a **code** (e.g. TR01)
and a **name** (e.g. *Tirana branch*). Inactive cost centres disappear from
the forms but stay in reports.

### Assigning

- **Sales and purchases:** on a saved order, the **Cost centre** bar at the
  top of the lines lets you choose one and **Save**.
- **Other income and expenses:** the Cash & Bank **Money in / Money out**
  form has a **Cost centre** field (when both modules are on).

### Report

**Reports → Cost centre report:** for the chosen period, per cost centre:

| Column | Comes from |
|---|---|
| Revenue | Sales without VAT, minus returns |
| Cost of goods | The average purchase cost of what was sold |
| Gross profit | Revenue − cost of goods |
| Purchases | Purchases without VAT |
| Other income / Expenses | Money in / out entered in Cash & Bank |
| **Result** | Revenue − cost of goods + other income − expenses |
| Previous period, % | The result of the period just before, of the same length, and the change |

Everything not assigned to a cost centre appears as **Not assigned**, so the
totals always match your other reports.

---

## 8e. Payroll, Fixed assets, Production, Tax due

All four are **optional modules, off by default**. Switch them on in
**Settings → Modules**. They appear under the **Finance & Operations**
(*Financa & Operacionet*) tab.

### Payroll (Listëpagesa)

1. **Employees:** name, personal number, position, **gross monthly salary**,
   start date (and end date if they left), IBAN, cost centre.
2. **New payroll:** choose the month and click **Create**. Every employee
   working that month gets a line with gross, employee social and health
   contributions, **income tax (TAP)**, **net**, employer contributions and
   **total cost**.
3. **Adjust:** change a gross amount (bonus, unpaid days) and click **Save
   and recalculate**.
4. **Approve:** after that it can't be changed. Its taxes then appear in
   **Tax due**.
5. **Mark as paid:** with Cash & Bank on, the net total is recorded as
   **Salaries** from the account you choose.

**Rates and tax** (Payroll → Rates and tax):
- **Contribution rates:** employee 9.5% + 1.7%, employer 15% + 1.7%.
- **Contribution base:** between the minimum wage (50,000) and the ceiling
  (186,416).
- **Income tax brackets:** tax = Fixed + Rate% × (gross − Over).
- **Example:** the page shows a worked example for 80,000 gross.

> The defaults follow Albania's 2026 rules as published, but they change by
> law. **Have your accountant confirm them before paying real salaries.**

### Payroll rules (2026, as the official list)

Arkon calculates every payslip exactly like the official **Listëpagesa**
(columns 10–21):

| Column | Rule |
|---|---|
| 11) Contribution base | gross, up to the ceiling **186,416** |
| 13) / 14) Social insurance | employer **15%** / employee **9.5%** (part time **002**: 13.92% / 9.38%) |
| 16) Health insurance | **1.7% + 1.7%** of the **full** gross |
| 19) Tax allowance | only if the employee **filed the yearly declaration**: gross up to 50,000 → the whole gross; 50,001–60,000 → 35,000; above → 30,000 |
| 20) Tax base | gross − allowance |
| 21) TAP | up to 170,000: **13%**; above: **22,100 + 23%** of the part over 170,000 |
| Net | gross − employee social − employee health − TAP |

- **TAP and the allowance are fixed by law** and can't be edited (Payroll
  settings shows them read-only). Contribution rates and the ceiling stay
  editable for future years.
- **Each employee** has a **Code at the tax office** (001 full time / 002
  part time) and **Has filed the yearly declaration**.
- **Calculate gross from net:** on the employee form, type the net you want
  and the button fills in the gross.
- **Listëpagesa (Excel):** on each payroll run, the run in the official
  column layout.

### Fixed assets (Aktivet afatgjata)

- **Register:** code, name, category, location, purchase date, **cost**,
  **salvage value**, **useful life in months**, method, cost centre.
- **Methods:**
  - **Straight-line:** (cost − salvage) ÷ months, every month.
  - **Declining balance:** a yearly % of the remaining value, monthly; at
    the end of the life it's written down to the salvage value.
- **Depreciation start:** the month after purchase.
- **List:** accumulated depreciation, **book value** on any date, and the
  depreciation of a chosen year, with totals.
- **Asset page:** click an asset for its month-by-month schedule. **Dispose
  / sell** with a date and sale value stops depreciation; it can be undone.

### Production (Prodhimi)

1. **Bill of materials:** choose the product you make and add its
   components with the quantity needed for **one** unit. The page shows what
   one unit costs to make at today's average costs.
2. **Production order:** product and quantity. The components are listed
   with their costs.
3. **Make it:** Arkon checks the components are in stock, then:
   - the components leave stock (**Used in production**);
   - the finished product enters stock (**Produced**);
   - the **production cost per unit** is saved and feeds the product's
     average cost, so profit on its sales is right.
4. **Reverse** undoes it, as long as the produced goods are still in stock.
   **Cancel** removes a draft.

If shelves are on, components are taken from stock that isn't on a shelf.

### Tax due (Detyrimet tatimore)

For each of the last 12 months:

- **VAT:** VAT on sales − VAT on purchases (returns included). A negative
  amount is a **credit**.
- **Payroll taxes** (with Payroll on): income tax + employee and employer
  contributions of approved payrolls.
- **Columns:** the **due date** (VAT on the 14th, payroll on the 20th of the
  next month, both editable), what's paid, what's **open**, and the status:
  due, overdue, paid, credit, or month running.

**Record a tax payment:** choose the tax, the month, the amount and the date.
With Cash & Bank on, choose the account and it's recorded as **Taxes**.

> These figures help you prepare; **your accountant files the returns**.

---

## 8f. Accounting

**Optional module, off by default.** Switch it on in **Settings → Modules**.
It's under **Finance & Operations → Accounting**.

**Access:** administrators, and the **Accountant** role by default (full
access here, since bookkeeping is their job). Other users can be given
access per user.

### Chart of accounts (Plani kontabël)

- **Pre-filled plan:** a compact plan following the Albanian classes (101
  capital, 401 suppliers, 411 clients, 4456/4457 VAT, 512 bank, 531 cash,
  607 cost of goods, 641 wages, 701 sales…).
- **Editing:** add your own accounts, rename any, and make unused ones
  inactive.
- **Automatic entries:** accounts marked **Automatic entries** are used by
  Arkon. You can rename them, but not delete them or change their type.

### Official financial statements (Pasqyrat financiare)

**Accounting → Official statements** builds the statements required by
**Law 25/2018**, in the official layout, for a year and the year before:

- **Pasqyra e Pozicionit Financiar** (balance sheet)
- **Pasqyra e Performancës** (by nature)
- **Pasqyra e Fluksit të Mjeteve Monetare** (indirect method)
- **Pasqyra e Ndryshimeve në Kapital** (changes in equity)

How it works:

- **Lines:** each line takes the accounts listed in the official template
  (e.g. *Mallra = 35; 395*). On the chart of accounts every account shows
  its **Line**. **Edit → Line in the official statements** sends an account
  elsewhere; Arkon's own accounts that differ from the official chart
  (605, 607, 281, 164, 442, 4456, 681, 890) already have their line set.
- **Result of the year:** shown on *Fitimi/(humbja) e periudhës*; earlier
  results on *Fitimi i pashpërndarë*.
- **Warnings** at the top list accounts that have a balance but no line,
  and tell you if the balance sheet doesn't balance.
- **Excel (official format)** fills the official template: the cover page
  (Kapaku), the four statements (this year and last year), and the account
  sheets (LLOGARITË 1–5 and 6–7). The template's own totals and checks stay
  as formulas.
  - **First:** enter the company details for the cover in **Settings →
    Company details for the financial statements** (legal form, date
    founded, main activity).
  - **Before exporting:** update the year's automatic entries in the
    journal.

**The national chart of accounts:** the full analytic chart (345 accounts)
is added to the chart of accounts as **inactive**. Activate the ones you
use. **Show all accounts** lists them.

### Accounting entries on each sale and purchase

With Accounting on, every saved sale and purchase page has an **Accounting
entries** panel. It shows the accounts the document moves (e.g. Dr 411
Clients / Cr 701 Sales + Cr 4457 VAT; Dr 607 / Cr 355), and an entry for each
return and each payment, with debit and credit totals. It's calculated live
with the journal's own rules, so you see it while the order is being made,
before the journal is updated. Users who can view Accounting see it.

### Automatic entries

Arkon creates the bookkeeping from what you already record:

| Source | Entry |
|---|---|
| Sale | Dr 411 Clients / Cr 701 Sales + Cr 4457 VAT; and Dr 607 Cost of goods / Cr 355 Goods |
| Purchase | Dr 355 Goods + Dr 4456 VAT / Cr 401 Suppliers |
| Returns | The same, reversed |
| Payments and refunds | Bank 512 or cash 531 against clients or suppliers |
| Cash & Bank entries | Rent, utilities, transport, bank fees, taxes, capital, loans, owner withdrawals, other income or expenses, transfers, corrections → their accounts |
| Payroll | Dr 641 Wages + Dr 644 Employer contributions / Cr 421 Net pay + Cr 431 Contributions + Cr 442 TAP; paying salaries clears 421 |
| Fixed assets | Monthly depreciation, Dr 681 / Cr 281 |
| Stock adjustments | Damage or loss Dr 658 / Cr 355; found goods the reverse, at average cost |
| Tax payments | VAT clears 4457; payroll taxes clear 442 and 431 |
| Cash and bank opening balances | Against 890 Opening |

**How to use it:** in the **Journal**, choose a period and click **Update
automatic entries**. Arkon rebuilds that period's automatic entries from the
current data, so later edits, returns or deletions are always reflected.
Run it again whenever data in that period changes. **Manual entries are
never touched.**

> **Not automatic:** buying a fixed asset, and VAT settlement between 4456
> and 4457. Record those as manual entries.

### Manual entries

**New entry:** a date, a description, and up to 8 lines (account, debit
**or** credit, note). The totals and the difference update as you type, and
the entry can only be saved when **debits equal credits**. Manual entries
can be edited and deleted; automatic ones can't (change their source
instead).

### Reports

- **General ledger (Libri i madh):** one account, the opening balance, each
  line, and the running balance.
- **Trial balance (Bilanci i verifikimit):** per account, the opening,
  period debits and credits, and the closing, with Excel export. Click an
  account to open its ledger.
- **Financial statements:**
  - the **income statement** for the period (income, expenses, result);
  - the **balance sheet** at its end (assets, liabilities, equity and the
    result not yet closed).
- **VAT books (Librat e TVSH):** the **sales book** and **purchase book** of
  a month, with every invoice and return, the party's **NIPT**, value
  without VAT, VAT and total.
  - **Excel – Tatimet:** downloads the book in the **tax office's own Excel
    format** (Libri i Shitjeve / Libri i Blerjeve, version 4). The header
    has the company, NIPT, year and month ("Pa veprimtari: PO" when there
    are no invoices). There's one row per invoice, with the template's
    formulas for VAT and totals, and more rows are added when needed.
    - **Sales by VAT rate:** 20% / 10% / 6% go in their columns. 0% sales go
      in the column chosen on the client's VAT panel: *Eksporte mallrash*
      (default), *Furnizime me 0%*, *Shitjet e përjashtuara* or *Shitjet pa
      TVSH*.
    - **Purchases** go in the columns of their **purchase type**:
      - goods from local suppliers: *Nga Furnitorë Vendas* 20 / 10 / 6%,
        or *Të përjashtuara* without VAT;
      - imported goods: *Importe mallra*;
      - local investments: *Të Investimit nga Furnitorë Vendas*;
      - imported investments: *Importe të investimit*;
      - farmers: *Nga Fermerët vendas*.

      "Nr Faturës" is the **supplier's invoice number** when it's entered.
    - **Fill in by hand** the invoice serial (fiscal) number and the
      district (Rrethi), which Arkon doesn't hold. Have your accountant
      check before filing.
  - **Excel:** the simple list as before.
- **Statements → Export to Excel:** the income statement and balance sheet
  in one sheet.

Entry descriptions (and the Cash & Bank descriptions made by Payroll and Tax
due) are written in the **invoice language** from Settings, like invoices,
because they're stored as records.

> These are management figures to help you and your accountant. The
> official statements and VAT returns are prepared and filed by your
> accountant.

---

## 8g. Statements, stock count, reminders, period lock, app

Each is an **optional module, off by default**. Switch them on in **Settings →
Modules**.

### Client and supplier statements (kartela)

**Reports → Client statement** or **Supplier statement:** choose the party
and the period.

- **What it shows:** the opening balance, then every invoice, return,
  payment and refund in date order, with the **balance after each line**,
  and the closing balance.
- **The balance line** says who owes whom: the client owes us, we owe the
  supplier, an overpayment, or settled.
- **Use it to agree balances** with clients and suppliers. **Print** it, or
  **Export to Excel**.
- **Access:** anyone with access to **Unpaid (debts)**.

### Stock count (inventarizimi)

**Reports → Stock count → Start.**

1. **Snapshot:** Arkon lists every product with what it **expects** in
   stock.
2. **Count:** **Print count sheet** gives a sheet with an empty line per
   product.
3. **Enter the numbers:** type what you actually counted (use the search to
   find products quickly), and **Save counts** as you go. Leave a product
   empty if you didn't count it.
4. **Apply corrections:** for every difference, Arkon creates a "Stock count:
   more / less than recorded" adjustment, against **today's** stock. Then the
   count closes.

If shelves are on and a product is on shelves, remove the missing quantity
from the right shelf in Stock adjustments first; Arkon tells you which
products.

### Notifications and payment reminders

**The bell** in the top bar shows alerts for what you have access to:

- client invoices that are overdue, or due soon;
- supplier invoices that are overdue;
- products below their limit;
- taxes overdue, or due within 7 days.

A red number shows how many. Click an alert to open the right page.

**Payment reminders** (Reports → Payment reminders) are emails to clients
with an email address, for sales with a due date:

- **Before the due date:** some days before it (default 3).
- **While overdue:** every so many days (default 7).
- **Sending:** the page lists what's due to be sent today; **Send now**
  sends them. Or tick **Send automatically every morning at 9:00** in
  Settings.
- **No repeats:** each reminder is logged, so it's never sent twice. Emails
  are written in the invoice language.
- **Needs:** email switched on (Administration → Email settings).

### Period lock and year-end closing

**Settings → Period lock:** set **Locked up to and including** a date.
Nothing dated on or before it can be added, changed or deleted:

- orders started in that period;
- payments, Cash & Bank, journal entries;
- stock adjustments, payroll, tax payments.

Updating automatic accounting entries skips the locked months. Clear the
date to unlock.

**Year-end closing** (Accounting → Statements, at the bottom): choose the
year and click **Close the year**.
- **The closing entry:** dated 31 December, it moves the year's income and
  expenses into 108 (retained result). The income statement still shows the
  year normally.
- **Lock too:** tick **Also lock the year** to set the period lock at the
  same time.
- **Running it again** replaces the earlier closing.

### Installable app

With this module on, browsers offer to **install Arkon** like an app:

- **Phone:** on Chrome on Android use *Add to home screen*; on Safari on
  iPhone use *Share → Add to Home Screen*.
- **Computer:** the install icon in the address bar.

It opens in its own window with the Arkon icon. Styles and images are kept
on the device so it opens quickly, but **pages and business data always come
from the server**; nothing is stored on the phone. Without a connection it
shows a "no connection" page. Switching the module off removes it again on
the next visit.

> Installing on phones needs the app to be reached over **HTTPS** (or on the
> same computer at localhost). Over plain http://192.168… it still works in
> the browser, just without the install option.

---

## 8h. Budgets, cash-flow forecast, two-step login, price lists

Each is an **optional module, off by default** (Settings → Modules).

### Budget and budget vs actual

**Finance & Operations → Budget vs actual** (the **Budget** tab to enter it).

- **Enter the budget:** amounts per month, without VAT, for:
  - sales, cost of goods sold and other income;
  - rent, salaries, utilities, transport, taxes and fees, bank fees, other
    expenses.

  The **Result** row adds itself up.
- **Quick start:** **Fill from last year's actual results**, with a
  percentage (e.g. +10).
- **Budget vs actual:** for a month and the **year to date**, each line's
  budget, actual, difference and %. **Green** means better than budget (more
  income, or lower costs). A chart compares the result month by month.
- **Where the actual numbers come from:**
  - sales and cost of goods: from sales;
  - other income and expenses: from Cash & Bank entries;
  - salaries: from approved payrolls, when Payroll is on.

### Cash-flow forecast

The expected **cash and bank balance for the next 13 weeks**, starting from
today's Cash & Bank balance:

- **In:** what clients owe, on each invoice's due date.
- **Out:**
  - what you owe suppliers;
  - taxes from Tax due;
  - salaries (repeating the last payroll);
  - the budgeted expenses, spread over each month.
- **Overdue amounts** are put in the first week.
- **The lowest point:** shown and marked red if it goes below zero, so you
  see a cash shortage coming.

### Two-step login

1. **Turn it on for users:** Settings → **Two-step login** → **Required for
   everyone** (or leave it optional).
2. **Each user sets it up:** in their **account menu → Two-step login**, they
   scan the QR code with an authenticator app (Google Authenticator,
   Microsoft Authenticator…) and type the 6-digit code to confirm.
3. **At each login:** after the password, Arkon asks for the current code.
   Five wrong codes end the login.

**Lost phone?** An administrator opens the user (Administration → Users) and
ticks **Reset two-step login**. The user then sets it up again with the new
phone.

### Price lists

**Administration → Price lists.**

1. **Create lists**, e.g. *Wholesale* or *Retail*.
2. **Set prices:** type a price per product (without VAT), or use **Set all
   prices to the sale price ± %** (e.g. −10). Empty means the normal sale
   price.
3. **Give a list to a client:** on the client's page (Administration →
   Clients), choose the **Price list**.

From then on, that client's **sales and offers use the list prices**
automatically. **Class discounts still apply on top**, so you can combine
the two (or set the class discount to 0).

---

## 8i. Multi-currency (EUR, USD)

**Optional module, off by default** (Settings → Modules → Multi-currency).

The base currency (Settings, usually **ALL**) stays the currency of every
report, the unpaid lists, statistics, profit, accounting and the VAT books.
Every amount is also kept in the base currency, so those all keep working as
before.

### Exchange rates

**Administration → Exchange rates:** enter the rate of the day, e.g.
**1 EUR = 98.50 ALL** and **1 USD = 91.20 ALL** (the Bank of Albania rate).
Arkon uses the latest rate on or before each date. Without a rate, it asks
you to enter one first.

### Orders in EUR or USD

1. **Choose the currency:** on a new sale, purchase or offer, choose
   **EUR** or **USD** before adding the first product. Today's rate is used.
   When choosing EUR or USD, a **Rate** field appears with today's rate;
   type another rate if you prefer.
2. **The currency bar:** the order page then shows the currency, the rate,
   and the total in that currency and in ALL. While nothing is paid you can
   **change the rate** there.
   - **Purchases:** the price paid per unit is typed in EUR or USD; stock
     cost and profit stay in ALL.
   - **Sales and offers:** priced from the product's ALL price (and price
     lists), converted at the order's rate.
3. **Payments:** typed in the order's currency, and they count at the
   order's rate, so the balance always adds up.
4. **Invoices and credit notes:** lines and totals are in the order's
   currency. The header shows the **currency and rate**, and a box at the
   bottom gives the **summary in ALL**: taxable supplies, VAT-free
   supplies, total VAT and total, as Albanian invoices show them.
5. **Offers:** an offer in EUR becomes a sale in EUR at the same rate.

### Exchange differences (automatic)

A payment in EUR or USD has a **Rate on payment day** field, filled in from
the daily rates. Type a different rate if your bank used another one.

- **Clearing the invoice:** the payment clears the invoice at the order's
  rate, so the invoice shows as fully paid.
- **Cash & Bank** gets the real amount: EUR in a EUR account, or the real
  lek in an ALL account.
- **The difference** is the **exchange gain or loss**. It shows on the
  payment, and Accounting books it to **769 Exchange gains** or **669
  Exchange losses**.

Example: an invoice of 1,000 EUR at 98.50 = 98,500 ALL, paid at 99.20 =
99,200 ALL, gives a **700 ALL gain**. Paying a supplier after the euro rose
gives a loss, because the euros cost you more lek.

**Why it matters even when you keep the euros:** your books are in lek. The
1,000 EUR you received is worth 99,200 ALL, but it cleared a debt booked at
98,500 ALL. Exchanging the euros later is a separate transfer, whose
difference is booked to 769 / 669 too.

**Reports → Exchange differences** lists every payment with a difference,
with the gains, losses and net for a period.

### Cash and bank accounts in EUR or USD

- **Account currency:** in Cash & Bank → Accounts, choose the account's
  **currency**. Its opening balance, entries and reconciling are in that
  currency, and its card shows the balance in EUR or USD with the ALL
  equivalent. Overview totals are converted to ALL at today's rate.
- **Exchanging money:** use a **Transfer** between accounts in different
  currencies, entering both the amount sent and the **amount received**
  (e.g. 100 EUR out, 9,850 ALL in). In accounting, any lek difference is
  booked as an exchange gain or loss.
- **Changing an account's currency:** only possible before it has
  movements.

### VAT per client and per sale

**Normally every sale uses the VAT rate in Settings (e.g. 20%).** Two
options change that when needed:

- **Per client** (Administration → Clients → a client → **VAT**): enter
  the client's own rate (e.g. **0**) and the **note** to print on their
  invoices (e.g. the exemption wording your accountant gives you). Every
  new sale and offer for that client uses it. Leave it empty for the normal
  rate.
- **Per sale or offer:** when the client has no VAT of its own, the order
  page shows a **VAT** bar.
  - **Change for this sale:** type a rate and a note, then **Save**. All
    lines are recalculated.
  - **Back to normal:** returns the order to the normal rate.
  - **Locked after payment:** VAT can be changed only before the first
    payment is recorded.

Each order keeps the rate it was made with, so changing a client's VAT later
doesn't change old invoices. Purchases always use the normal rate. The VAT
books, accounting and reports use each line's actual VAT.

> Which rate and wording apply (0% export of goods, services to foreign
> businesses, exemptions) depends on your case. Ask your accountant.

---

## 8j. Quick sale (POS)

**Optional module, off by default** (Settings → Modules → Quick sale).
Open it from **Sales → Quick sale (POS)** or the dashboard's quick actions.
It needs full access to Sales.

1. **Add products:** **scan** a barcode (the product code) or type a code and
   press **Enter**, or search by name and click a product. Out-of-stock
   products are greyed out.
2. **Cart:** change quantities with **−** / **+** or type them; **×**
   removes a line. Prices come from normal pricing, so the client's discounts,
   price lists and VAT apply.
3. **Client:** the default walk-in client (*Klient pakice*, created
   automatically), or type or choose another.
4. **Payment:** **Cash** or **Card**. For cash, type the amount received
   and Arkon shows the **change**. With Cash & Bank on, you can choose the
   account.
5. **Complete sale:** Arkon creates a normal sale, takes the stock (choosing
   a shelf automatically if needed) and records it as paid. **Everything or
   nothing:** if one product lacks stock, nothing is saved and you see why.
6. **Receipt:** **Print receipt** opens a narrow receipt for an 80 mm receipt
   printer (or normal paper). **New sale** starts again.

**Settings → Quick sale:** the company selling (empty = the first company)
and the default client.

> The receipt is an **internal document, not a fiscal receipt**. Albanian
> law requires fiscalized receipts for sales to customers; use your fiscal
> system for that until fiscalization is added through a licensed provider.

---

## 8k. Shops and warehouses

**Optional module, off by default** (Settings → Modules → Shops and
warehouses). When you switch it on, everything you have so far belongs to
the **main location** ("Kryesore"). Rename it in **Locations**, e.g. to
"Dyqani Tiranë".

### Locations

**Reports → Locations** (the tab on the location pages): add each **shop**
or **warehouse** with a code, a name and an address. Each has its **own
stock**.

### Who sells where

- **Users:** in Administration → Users, set where each user **works at**.
  Their sales, stock adjustments and quick sales (POS) happen there
  automatically.
- **New sales, purchases and offers:** have a **Location** field (the
  user's shop by default). The order page shows its location.
- **Stock checks are per location:** a shop can only sell what's in that
  shop. Purchases add stock to the location they're made at.
- **Returns** go back to the same location. **An offer** becomes a sale at
  the offer's location.

### Transfers

**Reports → Transfers:** choose **From** and **To**, then add the
products and quantities (up to 6 lines at a time).
- **Stock check:** Arkon checks there's enough at the sending location, then
  moves the goods.
- **The list:** every transfer, with what was moved.
- **Undo:** possible while the goods are still at the receiving location.

Transfers don't change the company's total stock or the accounting; they
only move goods between locations.

### Stock by location

**Reports → Stock by location:** every product with its quantity in each
location and the total. Use the search to find products.

### Also per location

- **Stock adjustments:** choose the location.
- **Stock count:** count one location at a time.
- **The POS:** shows and sells the stock of the user's shop.

> **Note:** shelves (if used) are not yet linked to locations; they work
> across the company. Use one or the other per product for now.

---

## 8l. Batches, expiry dates and serial numbers

**Optional module, off by default** (Settings → Modules → Batches and
serial numbers). Then choose **Tracking** on each product (Administration
→ Products → a product):

| Tracking | For | What it means |
|---|---|---|
| None | most products | as before |
| Batch and expiry date | food, cosmetics, medicines | stock is kept per batch |
| Serial numbers | electronics | every unit has its own number |

### Batch and expiry

- **Purchases:** the add-product form asks for the **batch number** and
  **expiry date**.
- **Sales:** Arkon takes the batch that **expires first** (*first expired,
  first out*), or you choose one from the **Batch** list, which shows each
  batch's expiry and quantity.
  - **Expired batches** are never picked and can't be chosen.
  - **Split quantities:** if no single batch has enough, split the quantity
    into lines.
- **Each line** on the order shows its batch and expiry.
- **Returns** go back to the same batch. **Transfers** keep the batch and
  expiry. **Stock adjustments** take from the first batch to expire.
- **Reports → Expiry report:** batches that have expired (red) or expire
  within a number of days (orange within 7), with quantity and location.

### Serial numbers

- **Purchases:** scan or type **one serial number per unit** (one per line)
  in the **Serial numbers** box. Their count must match the quantity, and a
  serial can't exist twice.
- **Sales:** scan the units you sell, or leave the box empty and Arkon takes
  the oldest units in stock (the POS does this automatically).
- **Each line** shows its serial numbers (SN: …).
- **Deleting a sale line** puts its units back in stock. **Deleting a
  purchase line** removes its units, unless some were already sold.
- **Returns:**
  - **from a client:** the units are back in stock;
  - **to a supplier:** they're marked returned.
- **Transfers** move the units to the new location.
- **Reports → Serial number lookup:** type all or part of a serial to see
  the product, its status (in stock, sold, returned, removed), the purchase
  and sale (with links and dates), and where it is.

---

## 9. Invoices

### Creating an invoice

Open an order and click **Create invoice**. The PDF contains:

- **Header:** your logo and company details, and the title (**SALE INVOICE**,
  **PURCHASE INVOICE** or **OFFER**) with its number, date, and due date or
  valid-until date.
- **Bill to:** the client's or company's details.
- **Highlighted amount:**
  - **Amount due:** nothing paid yet.
  - **Balance due:** partly paid.
  - **Paid in full:** settled.
  - **Offer total:** on offers.
- **Items:** quantity, unit price before discount, discount (only when used)
  and amount.
- **Totals:** subtotal, VAT with its rate, total, and on sales and purchases
  also paid and balance due.
- **Notes:** payment terms or offer validity.
- **Bottom:** Buyer and Seller signature lines, and a page number.

**Colour and font:** the accent colour is the **Invoice colour** from
Settings, and Albanian letters print correctly.

**Recreating:** creating the invoice again replaces the earlier PDF. Do this
after adding payments so the invoice shows the new balance.

### Emailing an invoice

On a sale or an offer, the **Email invoice** panel sends the invoice PDF to
the client:

1. **To:** the client's email is filled in; change it if needed.
2. **Message** (optional): leave it empty to use a standard greeting.
3. Click **Send invoice**. Arkon creates the current invoice and attaches it.
   Every email sent is recorded in the audit log.

Email is **optional**. The panel only appears when an administrator has
switched email on under **Administration → Email settings**. When email is
off, it's hidden and nothing about email shows on sales or offers.

When **Send emails from Arkon** is ticked, every field below is required. If
any is empty, saving is refused with a list of what's missing, and what you
typed stays in the form. With email unticked, you can save partial settings
to finish later.

| Field | What to enter |
|---|---|
| Send emails from Arkon | Tick to switch email on |
| SMTP server, port, security | Your mail provider's details. Gmail: `smtp.gmail.com`, 587, STARTTLS. Outlook/Office 365: `smtp.office365.com`, 587, STARTTLS |
| Username / password | Your email account. Gmail needs an **app password** (Google account → Security → App passwords), not your normal password. The password is never shown again; leave the field empty to keep it |
| Sender address | The address clients see the invoice from |

After saving, use **Send a test email** to check the settings work.

### The Invoices menu

It lists the saved PDFs per type, with live search by file name (client,
date or order number).

- **Open or download:** click the file name to open it, or **Download** to save
  it.
- **Delete:** administrators only. You can always create the invoice again from
  the order.

---

## 10. Reports

All reports are for administrators.

### Purchases report / Sales report

- **Three figure cards:** **Today**, **This month** and **This year**, each with
  orders, items, value without VAT, VAT and value with VAT.
- **Period:** choose today, this month, this year or a custom range, then
  **Show**. The table lists every line in that period, with a coloured totals
  row: value without VAT orange, VAT red, total green.
- **Export to Excel** and **Print.**

### Products report

Search by product name. It shows units bought,
sold and remaining, plus every matching sale and purchase line. Click a line to
open its order.

### Warehouse report

Choose **Start date** and **End date**, then one of two views and click
**Generate data**:

- **Warehouse inventory:** per product, units **purchased**, **sold** and
  **adjusted** in the period, units **remaining**, the sale price, and the
  value of what remains. Total value at the bottom.
- **For supply: less than [N] above the limit:** products to reorder, meaning
  those whose remaining stock is below their quantity limit or less than *N*
  units above it. *N* defaults to 3.

**Export to Excel** exports the view shown.

### Purchase orders (reordering)

**Where:** Reports → **For supply (reorder)** (or Warehouse report → For
supply), and Purchases → **Purchase orders** (also under Reports).

1. **Pick the products:** in the For supply list, tick the products to
   reorder. **To order** is pre-filled with enough to get back above the
   limit; change it if you like.
2. **Create it:** choose the **merchant** and your **company**, then click
   **Create purchase order**.
3. **Send it:** the purchase order (e.g. PO-00001) has a **PDF**, plus an
   **Email to the merchant** panel when email is on. While it's open, you can
   change quantities (0 removes a product) or add a note.
4. **Receive it:** when the goods arrive, click **Goods received**. Arkon
   creates the real purchase with these products and prices, and **only now
   does stock go up**. Or **Cancel order** if it's no longer needed.

---

## 11. Statistics

Choose a period at the top (today, this month, this year or a custom range).

**Choose what to show:** the **⚙ Charts** button lists every chart and
figure block with a tick box. Untick what you don't need; **Show all** and
**Hide all** do everything at once. The choice is remembered in that
browser, so each person can keep their own view. The Excel export always
contains everything.

### Headline figures

| Figure | How it's calculated |
|---|---|
| Sales (with VAT) | Sum of sale lines in the period; also number of orders and items |
| Purchases (with VAT) | Sum of purchase lines in the period |
| Estimated gross profit | Sales without VAT − cost of the goods sold. The margin % is profit ÷ sales without VAT |
| Average sale | Sales with VAT ÷ number of sales |
| Offers turned into sales | Offers converted ÷ offers made in the period |

**How profit uses real costs.** Each sale line stores the product's **average
purchase cost** when it was sold. That's the total value of all its purchases
divided by the total quantity purchased.

| Purchase | Quantity | Price paid |
|---|---|---|
| January | 100 | 180 |
| March | 100 | 200 |

The average cost is (100 × 180 + 100 × 200) ÷ 200 = **190**. A sale of 10 units
at 250 then earns (250 − 190) × 10 = **600** profit.

Sales made before costs were stored use the product's current buy price.

### Returns section

Below the charts, **Returns** shows, for the chosen period:

- **Returned by clients:** value, **% of sales**, number of returns and
  units.
- **Returned to merchants:** value, number of returns and units.

And five charts:

- which product was returned more;
- which client returned more;
- why clients returned goods (by reason);
- which merchant you returned more to;
- why you returned goods to merchants.

### Charts

- **Sales, purchases and offers per day or month:** per day for periods up to
  two months, otherwise per month.
- **Which client bought more:** pie chart, top 10 plus "Others".
- **Which product was sold more:** by units.
- **Who sold more:** by user.
- **Products that earned the most:** by value.
- **Sales by product class.**

**Export to Excel** gives one sheet per chart plus a summary sheet.

---

## 12. Administration

### Merchants, clients, companies, products

Add, edit and delete. Deleting doesn't change past orders; they keep the names
they were saved with.

### Users

- **Fields:** first and last name, email, username, role and password. The
  password must be at least 8 characters; leave it empty when editing to keep
  the current one.
- **Your own account:** you can't delete it or remove your own administrator
  role.

### Settings

| Section | Options |
|---|---|
| Money and invoices | Currency; VAT % (applies to lines added from now on, and saved lines keep theirs); invoice colour |
| Invoice logo | Upload, replace or remove |
| Invoice language | **Shqip** or **English** for invoices, credit notes, purchase orders and pick lists (the screen language is separate, per user) |
| Payment terms and offers | Default days until payment is due; default days an offer stays valid |
| Background | Two pastel gradient colours; watermark icon strength; panel opacity (live preview; **Reset to default**) |

### Backup and restore

**Where:** Administration → **Backup and restore**.

- **Export backup:** saves everything (sales, purchases, offers, payments,
  products, shelves, users, settings, audit log, invoice PDFs and logo) into
  one `.zip` file. In Chrome, Edge and Brave a **Save as** window asks where
  to put it; other browsers save it to Downloads. Keep copies on another
  disk or in cloud storage.
- **Restore:** choose a backup file, tick the confirmation box, and click
  **Restore backup**. **All current data is replaced** by the backup's:
  - **Safety copy first:** Arkon saves a copy of the current data in the
    `backups` folder of the data directory.
  - **All or nothing:** the data is replaced in one step; if anything goes
    wrong, nothing changes.
  - **Log in again:** everyone is logged out afterwards. Users and passwords
    are now the ones from the backup.
  - **Same version only:** a backup can only be restored into the same Arkon
    version it came from. The page says so if they differ.

### Audit log

A record of **who did what, and when**. It includes:

- logins;
- order lines added and removed, and orders deleted;
- invoices created and deleted;
- payments added and deleted, and due dates changed;
- offer status changes and offers turned into sales;
- stock adjustments and undos;
- products, merchants, clients, companies and users created, changed or
  deleted;
- settings changes and password changes.

**Searching:** live, by user, action, order number, product or any detail, and
by date range.

**Clear all logs** deletes every entry, after confirmation. The clearing itself
becomes the first entry of the new log, recording who cleared it, when, and
how many entries were deleted.

---

## 13. Common situations

| Situation | What to do |
|---|---|
| A sale was entered with the wrong quantity | Open it, **Remove** the line and add it again |
| A whole order is wrong | An administrator deletes it from the list (its payments go with it) |
| A client returns goods from a sale | Open the sale → **Returns** → quantities and reason → **Record return** |
| Sending broken goods back to a merchant | Open the purchase → **Returns** → quantities and reason → **Record return** |
| Which products come back most, and why? | Statistics → **Returns** |
| Paid back a client after a return | Sale → Payments → **Record refund** |
| Need to reorder | Warehouse report → **For supply** → tick products → **Create purchase order** |
| Keep a copy of everything | Administration → **Backup and restore** → **Export backup** |
| Goods were damaged in the warehouse | Adjustment → **Damaged** |
| The shelf count differs from Arkon | Adjustment → **Stock count: more / less than recorded** |
| A client paid half | Record a payment for the amount received; the order shows **Partly paid** |
| A client asks for more time | Change the **Due date** in the Payments panel |
| The client accepted an offer | **Turn into sale** on the offer |
| A supplier charged a different price this time | Fill in **Price paid per unit** on the purchase line |
| A product needs reordering | Home → **To restock**, or Warehouse report → **For supply** |
| Who changed or deleted something? | Administration → **Audit log**, search for the order number or product |
| Can't sell: "Not enough stock" | Record the purchase first, or check whether a stock adjustment is needed |
| The invoice shows an old balance | Click **Create invoice** again after recording the payment |
| Where is this product? | Administration → Products, **Shelves** column, or the Shelves page |
| Goods need to be collected for a sale | Open the sale → **Pick list** or **Pick list PDF** |
| Moved goods to another shelf | Shelves → **Place or move stock** (shelf to shelf) |
| Don't want to use shelves | Settings → Shelves → untick **Use shelves** |
| Too many columns on a list | **☰ Columns** above the grid |
| How much is in the bank / the till? | **Arka & Banka** → Overview |
| Paid rent or salaries | **Arka & Banka** → **− Money out** |
| Deposited cash into the bank | **Arka & Banka** → **Transfer** (Arka → Banka) |
| The till count doesn't match | **Arka & Banka** → **Reconcile** |
