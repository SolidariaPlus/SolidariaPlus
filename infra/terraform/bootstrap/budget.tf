# Budget alarms at 50 % and 80 % of the account credit (KAN-2 OI-11). They are the safety net of
# session mode: the stop is manual, and a forgotten NAT gateway or Aurora cluster keeps charging.

resource "aws_budgets_budget" "credit" {
  count = var.budget_email == "" ? 0 : 1

  name         = "${local.name}-credit"
  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "ANNUALLY"

  cost_types {
    include_credit = false
    include_refund = false
  }

  dynamic "notification" {
    for_each = [50, 80]

    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = "ACTUAL"
      subscriber_email_addresses = [var.budget_email]
    }
  }
}
