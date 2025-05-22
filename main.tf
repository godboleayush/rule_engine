# AWS Provider Configuration
provider "aws" {
  region = "ap-south-1"  # AWS Mumbai region
}

# IAM Role for Lambda Execution
resource "aws_iam_role" "lambda_exec" {
  name = "springboot_lambda_exec_v2"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# IAM Role Policy Attachment for Lambda Execution (CloudWatch Logs)
resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_s3" {
  role       = aws_iam_role.lambda_exec.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# Lambda Function Configuration (Using ZIP file)
resource "aws_lambda_function" "springboot_lambda" {
  function_name = "SpringBootLambda"
  runtime       = "java21"  # Java 21 runtime
  handler       = "org.example.StreamLambdaHandler"  # Fully qualified class name
  memory_size   = 1024  # 1 GB memory
  timeout       = 30    # Timeout in seconds

  # Pointing to the ZIP file (make sure it's in the same directory as your Terraform files)
  filename         = "${path.module}/target/untitled-1.0-SNAPSHOT-lambda-package.zip"  # ZIP file path
  source_code_hash = filebase64sha256("${path.module}/target/untitled-1.0-SNAPSHOT-lambda-package.zip")

  role = aws_iam_role.lambda_exec.arn

  environment {
    variables = {
      S3_BUCKET_NAME = "ayush2604"  # Replace with your actual bucket
    }
  }
}

# Step 1: Create a REST API (API Gateway)
resource "aws_api_gateway_rest_api" "rest_api" {
  name        = "springboot-api"
  description = "API Gateway REST API for Spring Boot Lambda"
}

# Step 2: Create a Resource with {proxy+} Path (captures all paths)
resource "aws_api_gateway_resource" "default_resource" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  parent_id   = aws_api_gateway_rest_api.rest_api.root_resource_id
  path_part   = "{proxy+}"  # This will allow any path to be captured
}

# Step 3: Create a Method for the Resource with Lambda Proxy Integration
resource "aws_api_gateway_method" "default_method" {
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  resource_id   = aws_api_gateway_resource.default_resource.id
  http_method   = "ANY"  # Allowing all HTTP methods (GET, POST, PUT, etc.)
  authorization = "NONE"  # No authentication required
}

# Step 4: Integration of the Method with Lambda (Lambda Proxy Integration)
resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.rest_api.id
  resource_id             = aws_api_gateway_resource.default_resource.id
  http_method             = aws_api_gateway_method.default_method.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"  # Lambda Proxy Integration
  uri                     = aws_lambda_function.springboot_lambda.invoke_arn
}

# Step 5: Create and Configure a New Stage (Instead of using stage_name in Deployment)
resource "aws_api_gateway_stage" "default_stage" {
  stage_name   = "prod"  # Deployment stage (prod)
  rest_api_id  = aws_api_gateway_rest_api.rest_api.id
  deployment_id = aws_api_gateway_deployment.default_deployment.id
}

# Step 6: Deploy the API to a New Stage
resource "aws_api_gateway_deployment" "default_deployment" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  # Trigger the deployment only when resources (methods) are created
  depends_on = [
    aws_api_gateway_method.default_method,  # Ensure the method exists before deploying
    aws_api_gateway_integration.lambda_integration  # Ensure integration exists before deployment
  ]
}

# Step 7: Lambda Permission for API Gateway to Invoke Lambda
resource "aws_lambda_permission" "allow_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.springboot_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.rest_api.execution_arn}/*/*"  # Allow all HTTP methods and paths
}

# Outputs: Lambda function name and API Gateway URL (using the correct output from stage)
output "lambda_function_name" {
  value = aws_lambda_function.springboot_lambda.function_name
}

output "api_gateway_url" {
  value = "${aws_api_gateway_rest_api.rest_api.execution_arn}/prod"  # Correct URL for accessing the API
}
