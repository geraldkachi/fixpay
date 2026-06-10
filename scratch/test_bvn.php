<?php

require __DIR__ . '/../fixpay-laravel/vendor/autoload.php';
$app = require_once __DIR__ . '/../fixpay-laravel/bootstrap/app.php';
$kernel = $app->make(Illuminate\Contracts\Http\Kernel::class);

$user = App\Models\AppUser::where('email', 'test.userA@fixpay.test')->first();

$request = Illuminate\Http\Request::create('/api/kyc/bvn', 'POST', ['bvn' => '12345678901', 'dob' => '1990-01-01']);
$request->setUserResolver(fn() => $user);

try {
    $response = app(App\Http\Controllers\User\KycController::class)->verifyBvn($request);
    echo "Response:\n";
    echo $response->getContent();
} catch (\Illuminate\Validation\ValidationException $e) {
    echo "Validation Error:\n";
    echo json_encode($e->errors());
} catch (\Exception $e) {
    echo "Exception:\n";
    echo $e->getMessage() . " at " . $e->getFile() . ":" . $e->getLine() . "\n";
}
