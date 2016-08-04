<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument;

use Core\Di\Converter\Argument\Type\ArrayType;
use Core\Di\Converter\Argument\Type\BoolType;
use Core\Di\Converter\Argument\Type\FloatType;
use Core\Di\Converter\Argument\Type\IntType;
use Core\Di\Converter\Argument\Type\ObjectType;
use Core\Di\Converter\Argument\Type\StringType;

/**
 * Class TypeFactory
 */
class TypeFactory implements TypeFactoryInterface
{
    /**
     * @var array
     */
    protected $types = [
        ArrayType::TYPE_ARRAY => ArrayType::class,
        BoolType::TYPE_BOOL => BoolType::class,
        FloatType::TYPE_FLOAT => FloatType::class,
        IntType::TYPE_INT => IntType::class,
        ObjectType::TYPE_OBJECT => ObjectType::class,
        StringType::TYPE_STRING => StringType::class
    ];

    /**
     * @var TypeConverterInterface[]
     */
    protected $instances;

    /**
     * @inheritdoc
     */
    public function create($type)
    {
        if (!isset($this->types[$type])) {
            throw new \Exception(sprintf('Undefined type (%s) for converter instance', $type));
        }

        if (!isset($this->instances[$type])) {
            $this->instances[$type] = new $this->types[$type]($this);
        }

        return $this->instances[$type];
    }
}
