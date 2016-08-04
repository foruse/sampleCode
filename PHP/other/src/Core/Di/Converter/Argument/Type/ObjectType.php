<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\InstanceClass;
use Core\Di\Converter\Argument\TypeConverterInterface;

/**
 * Class ObjectType
 */
class ObjectType extends AbstractType implements TypeConverterInterface
{
    const TYPE_OBJECT = 'object';
    const ARGUMENTS = 'arguments';
    const CLASS_NAME = 'class';
    const SHARED = 'shared';

    /**
     * @param array $data
     * @return InstanceClass
     */
    public function convert(array $data)
    {
        if (!isset($data[self::CLASS_NAME])) {
            throw new \InvalidArgumentException('Key "class" must be set.');
        }

        $instanceArguments = [];
        $class = trim($data[self::CLASS_NAME], '\\');

        $arguments = isset($data[self::ARGUMENTS])
            ? $data[self::ARGUMENTS]
            : [];

        foreach ($arguments as $name => $argument) {

            if (!isset($argument[TypeConverterInterface::TYPE])) {
                throw new \InvalidArgumentException('Wrong or not exist type for "' . $name . '" argument.');
            }

            $instanceArguments[$name] = $this->typeFactory->create($argument[TypeConverterInterface::TYPE])
                ->convert($argument);
        }

        return new InstanceClass(
            $class,
            $class,
            $instanceArguments,
            (isset($item[self::SHARED]) ? (bool)(int) $item[self::SHARED] : false)
        );
    }
}
