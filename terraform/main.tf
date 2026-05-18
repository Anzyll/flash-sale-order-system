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
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 3000
    to_port     = 3000
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
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
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
  username             = "postgres"
  password             = "password123"

  publicly_accessible  = true
  skip_final_snapshot  = true
}