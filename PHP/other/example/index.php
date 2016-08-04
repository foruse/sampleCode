<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
require_once __DIR__ . '/../vendor/autoload.php';

use App\Application;
use App\Decorator;
use App\Printer;
use App\PrinterInterface;
use Core\Di\Config;
use Core\Di\Converter;
use Core\Di\Converter\Argument\Type\AbstractType;
use Core\Di\Converter\Argument\Type\ObjectType;
use Core\Di\Converter\Argument\Type\StringType;
use Core\Di\Converter\Argument\TypeConverterInterface;
use Core\Di\Converter\Argument\TypeFactory;

$configuration = [
    Converter::INSTANCES => [
        [
            ObjectType::CLASS_NAME => Application::class,
            ObjectType::ARGUMENTS => [
                'printer' => [
                    TypeConverterInterface::TYPE => ObjectType::TYPE_OBJECT,
                    ObjectType::CLASS_NAME => Decorator::class,
                    ObjectType::ARGUMENTS => [
                        'textBefore' => [
                            TypeConverterInterface::TYPE => StringType::TYPE_STRING,
                            AbstractType::VALUE => '=======================' . PHP_EOL,
                        ],
                        'textAfter' => [
                            TypeConverterInterface::TYPE => StringType::TYPE_STRING,
                            AbstractType::VALUE => '+++++++++++++++++++++++' . PHP_EOL,
                        ],
                    ]
                ]
            ],
        ],
        [
            ObjectType::CLASS_NAME => Printer::class,
            ObjectType::ARGUMENTS => [
                'text' => [
                    TypeConverterInterface::TYPE => StringType::TYPE_STRING,
                    AbstractType::VALUE => 'Hello world!!!' . PHP_EOL,
                ]
            ],
        ]
    ],
    Converter::INTERFACES => [
        PrinterInterface::class => Printer::class
    ]
];

$config = new Config($configuration, new Converter(new TypeFactory()));

$container = new \Core\Di\Container($config);

$app = $container->create(Application::class);

$app->run();
