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
class FloatType extends AbstractType implements TypeConverterInterface
{
    const TYPE_FLOAT = 'float';

    /**
     * @param array $data
     * @return float
     */
    public function convert(array $data)
    {
        return (float) $data[self::VALUE];
    }
}
