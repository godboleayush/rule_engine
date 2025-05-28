# =======================
# AWS PROVIDER
# =======================
provider "aws" {
  region = "ap-south-1"  # Mumbai region
}

# =======================
# IAM ROLE FOR LAMBDA
# =======================
resource "aws_iam_role" "lambda_exec" {
  name = "springboot_lambda_exec_v3"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Principal = {
          Service = "lambda.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# Allow CloudWatch logging
resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Allow S3 access
resource "aws_iam_role_policy_attachment" "lambda_s3" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# =======================
# LAMBDA FUNCTION
# =======================
resource "aws_lambda_function" "springboot_lambda" {
  function_name = "SpringBootLambda"
  runtime       = "java21"
  handler       = "org.example.StreamLambdaHandler"
  memory_size   = 1024
  timeout       = 30

  filename         = "${path.module}/target/untitled-1.0-SNAPSHOT-lambda-package.zip"
  source_code_hash = filebase64sha256("${path.module}/target/untitled-1.0-SNAPSHOT-lambda-package.zip")

  role = aws_iam_role.lambda_exec.arn

  publish = true  # ✅ Automatically publishes a version on deploy

  environment {
    variables = {
      S3_BUCKET_NAME = "ayush2604"
    }
  }

  # ✅ Enable SnapStart
  snap_start {
    apply_on = "PublishedVersions"
  }
}

# =======================
# LAMBDA ALIAS (POINTS TO PUBLISHED VERSION)
# =======================
resource "aws_lambda_alias" "springboot_lambda_alias" {
  name             = "prod"
  function_name    = aws_lambda_function.springboot_lambda.function_name
  function_version = aws_lambda_function.springboot_lambda.version  # ✅ Auto-published version
}

# =======================
# API GATEWAY
# =======================
resource "aws_api_gateway_rest_api" "rest_api" {
  name        = "springboot-api"
  description = "API Gateway REST API for Spring Boot Lambda"
}

# Create proxy resource (/{proxy+})
resource "aws_api_gateway_resource" "default_resource" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  parent_id   = aws_api_gateway_rest_api.rest_api.root_resource_id
  path_part   = "{proxy+}"
}

# Support ANY method
resource "aws_api_gateway_method" "default_method" {
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  resource_id   = aws_api_gateway_resource.default_resource.id
  http_method   = "ANY"
  authorization = "NONE"
}

# Link API Gateway to Lambda alias
resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.rest_api.id
  resource_id             = aws_api_gateway_resource.default_resource.id
  http_method             = aws_api_gateway_method.default_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_alias.springboot_lambda_alias.invoke_arn  # ✅ Using alias
}

# Deploy the API
resource "aws_api_gateway_deployment" "default_deployment" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id

  depends_on = [
    aws_api_gateway_method.default_method,
    aws_api_gateway_integration.lambda_integration
  ]
}

# Create prod stage
resource "aws_api_gateway_stage" "default_stage" {
  stage_name    = "prod"
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  deployment_id = aws_api_gateway_deployment.default_deployment.id
}

# Allow API Gateway to call Lambda alias
resource "aws_lambda_permission" "allow_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_alias.springboot_lambda_alias.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.rest_api.execution_arn}/*/*"
}

# =======================
# OUTPUTS
# =======================
output "lambda_function_name" {
  value = aws_lambda_function.springboot_lambda.function_name
}

output "lambda_alias_arn" {
  value = aws_lambda_alias.springboot_lambda_alias.arn
}

output "api_gateway_url" {
  value = "${aws_api_gateway_rest_api.rest_api.execution_arn}/prod"
}
