provider "aws" {
  region = "ap-south-1"
}

resource "aws_key_pair" "flash_sale_key" {
  key_name   = "flash-sale-key"
  public_key = file("flash-sale-key.pub")
}

resource "aws_instance" "flash_sale_server" {
  ami           = "ami-0f58b397bc5c1f2e8"
  instance_type = "t3.small"
  vpc_security_group_ids = [aws_security_group.flash_sale_sg.id]
  key_name = aws_key_pair.flash_sale_key.key_name
  tags = {
    Name = "flash-sale-server"
  }
}

resource "aws_security_group" "flash_sale_sg" {
  name = "flash-sale-security-group"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description      = "Flash Sale App Traffic"
    from_port        = 8085
    to_port          = 8085
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"] # Allows access from anywhere
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
resource "aws_db_instance" "flash_sale_db" {
  allocated_storage    = 20
  db_name              = "flashsale"
  engine               = "postgres"
  engine_version       = "17"
  instance_class       = "db.t3.micro"
  username             = var.db_username
  password             = var.db_password
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  publicly_accessible  = true
  skip_final_snapshot  = true
}

resource "aws_security_group" "rds_sg" {
  name = "flash-sale-rds-sg"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"

    security_groups = [aws_security_group.flash_sale_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"

    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "redis_sg" {
  name        = "flash-sale-redis-security-group"
  description = "Allow inbound traffic from the EC2 instance group only"

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    # This locks down Redis down so ONLY your EC2 app server can talk to it
    security_groups = [aws_security_group.flash_sale_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elasticache_cluster" "flash_sale_redis" {
  cluster_id           = "flash-sale-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  port                 = 6379

  security_group_ids   = [aws_security_group.redis_sg.id]

  tags = {
    Name = "flash-sale-redis"
  }
}