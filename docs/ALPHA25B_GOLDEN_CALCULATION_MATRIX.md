# Alpha 25B Golden Calculation Matrix

These examples are owner-phone spot checks and regression fixtures. All values are before recorded payments/discounts.

| Case | Principal | Terms | Start → End | Expected interest | Expected total |
|---|---:|---|---|---:|---:|
| P1 | ₹10,000 | 2%/month, Months+Days | 01-Jan → 01-Feb | ₹200.00 | ₹10,200.00 |
| P2 | ₹10,000 | 2%/month, Exact Days | 01-Jan → 16-Jan (15 days) | ₹100.00 | ₹10,100.00 |
| P3 | ₹10,000 | 2%/month, Full Month Started | 01-Jan → 02-Jan | ₹200.00 | ₹10,200.00 |
| P4 | ₹10,000 | 2%/month, Months+Days | 01-Jan → 16-Feb | ₹300.00 | ₹10,300.00 |
| F1 | ₹10,000 | Flat ₹300/month, 3 full months | 10-Jan → 10-Apr | ₹900.00 | ₹10,900.00 |
| F2 | ₹50,000 | Flat ₹300/month, Exact Days, 10 days | 01-May → 11-May | ₹100.00 | ₹50,100.00 |
| C1 | ₹10,000 | 2%/month, compound monthly | 01-Jan → 01-Mar | ₹404.00 | ₹10,404.00 |
| C2 | ₹10,000 | 2%/month, compound every 3 months | 01-Jan → 01-Jul | ₹1,236.00 | ₹11,236.00 |
| E1 | ₹10,000 | 2%/month | 31-Jan → 28-Feb 2026 | ₹200.00 | ₹10,200.00 |
| E2 | ₹10,000 | 2%/month | 29-Feb → 29-Mar 2028 | ₹200.00 | ₹10,200.00 |
| Z1 | ₹9,876.54 | 0%/month | 01-Jan-2026 → 01-Jan-2027 | ₹0.00 | ₹9,876.54 |

## Owner phone checklist

For each case:
1. Enter principal and start date.
2. Select the exact interest mode/rule.
3. Set preview/settlement date.
4. Compare monthly charge, interest and total to this table.
5. Save entry, reopen it and confirm the same terms are reconstructed.
6. Restart the app and confirm the result remains identical.

Any mismatch of even ₹0.01 is a blocker until the rule or expected result is explicitly corrected and documented.
