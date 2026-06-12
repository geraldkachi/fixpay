<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Symfony\Component\HttpFoundation\Response;
use App\Models\IdempotentRequest;

class IdempotencyMiddleware
{
    public function handle(Request $request, Closure $next): Response
    {
        // Only apply to mutating requests
        if (in_array($request->method(), ['GET', 'HEAD', 'OPTIONS'])) {
            return $next($request);
        }

        // Webhook endpoints often cannot send custom headers
        if ($request->is('api/webhooks/*')) {
            return $next($request);
        }

        $key = $request->header('X-Idempotency-Key');

        if (!$key) {
            return response()->json(['message' => 'X-Idempotency-Key header is required for mutating requests.'], 400);
        }

        // Validate UUID format
        if (!preg_match('/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i', $key)) {
            return response()->json(['message' => 'X-Idempotency-Key must be a valid UUID.'], 400);
        }

        $userId = $request->user()?->id;

        $idempotentRequest = IdempotentRequest::where('idempotency_key', $key)->first();

        if ($idempotentRequest) {
            // Prevent users from using keys belonging to other users
            if ($idempotentRequest->user_id && $userId && $idempotentRequest->user_id !== $userId) {
                return response()->json(['message' => 'Idempotency key mismatch.'], 403);
            }

            if ($idempotentRequest->status === 'PROCESSING') {
                return response()->json(['message' => 'Transaction is currently processing.'], 409);
            }

            return response()->json(
                $idempotentRequest->response_body,
                $idempotentRequest->response_code
            );
        }

        // Acquire concurrent request lock using Cache
        $lockKey = "idempotency_lock:{$key}";
        if (!Cache::add($lockKey, true, 30)) {
            return response()->json(['message' => 'Concurrent request in progress.'], 409);
        }

        try {
            // Create processing record
            $idempotentRequest = IdempotentRequest::create([
                'idempotency_key' => $key,
                'user_id' => $userId,
                'request_path' => $request->path(),
                'request_method' => $request->method(),
                'status' => 'PROCESSING',
            ]);

            // Inject idempotency key into request inputs to preserve any legacy controller behavior
            $request->merge(['idempotency_key' => $key]);

            $response = $next($request);

            // Only cache successful or client-error responses (2xx, 4xx).
            // Do not cache 5xx server errors so they can be retried safely.
            if ($response->getStatusCode() < 500) {
                $idempotentRequest->update([
                    'response_code' => $response->getStatusCode(),
                    'response_body' => json_decode($response->getContent(), true),
                    'status' => 'COMPLETED',
                ]);
            } else {
                $idempotentRequest->delete();
            }

            return $response;
        } finally {
            Cache::forget($lockKey);
        }
    }
}
