# Accounting rules (Phase 3)

## Inventory costing: weighted average cost
On every purchase, the product's cost (shown as purchase price) becomes
`(existing stock x current cost + landed cost of new goods) / (existing stock + new quantity)`.
Landed cost = line amount + its share of extra expenses - its share of invoice discount
(shares are proportional to line amounts; the last line takes the rounding remainder).
Zero or negative existing stock counts as no value.

## Sales
- Sale total = sum of (quantity x price - item discount) - invoice discount.
- Cost of goods sold (COGS) = quantity x average cost at the moment of sale, stored on the sale.
- Gross profit = sale total - COGS. A later change of purchase price never changes old sales.

## Dashboard definitions
- Sales / Profit: selected date range. Receivables, payables, stock: current totals.
- Receivables = sum(total - paid) over sales. Payables = sum(total - paid) over purchases.
- Cash balance = money received from sales - money paid for purchases (expenses and opening cash come later).
- Revenue, cash received, credit given and credit taken are kept separate in the data.

## Not yet included
Sales returns, purchase returns, customer/supplier payments after the invoice, expenses.
