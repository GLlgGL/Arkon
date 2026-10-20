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

| Tab | What's in it | Who sees it |
|---|---|---|
| Home | Dashboard strip and the eight tiles | Everyone |
| Purchases / Sales / Offers | **New …** and **List of …** | Everyone |
| Reports | Purchases, sales, products and warehouse reports; unpaid by clients / to merchants | Administrators |
| Statistics | Charts and headline figures | Administrators |
| Invoices | Saved PDF invoices | Everyone (purchase invoices: administrators) |
| Administration | Merchants, clients, companies, products, classes, returns and stock adjustments, users, settings, audit log | Administrators |

Menus open when you point at them with the mouse, or when you click or tap
them.

**Small windows, tablets and phones.** The app adapts to the width of the
window:

- **Below about 1380 px wide:** the tabs fold into a **☰ menu button** next
  to the logo. Tap it to open the menu; submenus open in place. The menu
  closes when you pick a page.
- **Phones:** buttons, filters and form fields become full-width rows, and
  the bar shows just the **Arkon** badge and your initials.
- **Grids:** wide grids scroll sideways inside their box. Use **☰ Columns**
  to hide what you don't need.

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

**Dashboard strip** (above the tiles). Each figure is a link to the page
behind it.

| Figure | Meaning |
|---|---|
| Sales today | Value with VAT and number of sales today |
| Unpaid by clients | What clients still owe. **Red** when some invoices are past their due date |
| To restock | Products whose stock is below their quantity limit (administrators) |
| Open offers | Offers that are still open: not accepted, rejected, expired or converted |

Regular users see their own figures. Below the strip are the eight tiles; for
regular users the administrator tiles are greyed out.

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
  without VAT, VAT and total. Excel export.

Entry descriptions (and the Cash & Bank descriptions made by Payroll and Tax
due) are written in the **invoice language** from Settings, like invoices,
because they're stored as records.

> These are management figures to help you and your accountant. The
> official statements and VAT returns are prepared and filed by your
> accountant.

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
