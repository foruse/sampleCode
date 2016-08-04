<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\Converter\Argument\TypeConverterInterface;

/**
 * Class IntType
 */
class IntType extends AbstractType implements TypeConverterInterface
{
    const TYPE_INT = 'int';

    /**
     * @param array $data
     * @return int
     */
    public function convert(array $data)
    {
        return (int) $data[self::VALUE];
    }
}
