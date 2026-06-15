const fs = require("fs");
const replace = (file, search, replace) => fs.writeFileSync(file, fs.readFileSync(file, "utf8").replace(search, replace));

// src/components/feature/CategoryBreakdownChart.tsx
replace("fixpay-pwa/src/components/feature/CategoryBreakdownChart.tsx", "import { formatCurrency }", "//import { formatCurrency }");
replace("fixpay-pwa/src/components/feature/CategoryBreakdownChart.tsx", "({ name, percent, value, fill }, index) =>", "({ name, percent, value, fill }) =>");

// src/components/feature/PaymentMethodsChart.tsx
replace("fixpay-pwa/src/components/feature/PaymentMethodsChart.tsx", "(entry, index) =>", "(entry) =>");

// src/components/feature/PaymentMethodSelector.tsx
replace("fixpay-pwa/src/components/feature/PaymentMethodSelector.tsx", "import React, { useState }", "import { useState }");

// src/components/feature/RepeatPaymentBottomSheet.tsx
replace("fixpay-pwa/src/components/feature/RepeatPaymentBottomSheet.tsx", "import { cn, formatCurrency, formatDateFull }", "import { formatCurrency, formatDateFull }");

// src/lib/api.ts
replace("fixpay-pwa/src/lib/api.ts", "let refreshing = false\nlet queue: any[] = []", "");

// src/mocks/handlers/analytics.ts
replace("fixpay-pwa/src/mocks/handlers/analytics.ts", "import { env } from '@/lib/env'", "const env = { API_URL: \"\" };");

// src/modules/auth/LoginScreen.tsx
replace("fixpay-pwa/src/modules/auth/LoginScreen.tsx", "user.kycStatus === 'VERIFIED'", "user.kycStatus === 'verified'");

// src/modules/home/HomeScreen.tsx
replace("fixpay-pwa/src/modules/home/HomeScreen.tsx", "import { TransactionItem }", "//import { TransactionItem }");
replace("fixpay-pwa/src/modules/home/HomeScreen.tsx", "import { walletService }", "//import { walletService }");

// src/modules/more/AnalyticsScreen.tsx
replace("fixpay-pwa/src/modules/more/AnalyticsScreen.tsx", "analytics.payment_methods_breakdown", "[]");

// src/modules/more/DisputesScreen.tsx
replace("fixpay-pwa/src/modules/more/DisputesScreen.tsx", "import { formatCurrency }", "//import { formatCurrency }");
replace("fixpay-pwa/src/modules/more/DisputesScreen.tsx", "import type { Dispute }", "//import type { Dispute }");

// src/modules/more/RaiseDisputeScreen.tsx
replace("fixpay-pwa/src/modules/more/RaiseDisputeScreen.tsx", "import { Input }", "//import { Input }");

// src/modules/send/SendScreen.tsx
replace("fixpay-pwa/src/modules/send/SendScreen.tsx", "const user = useAuthStore(s => s.user)", "");
