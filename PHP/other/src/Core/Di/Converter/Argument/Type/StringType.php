<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\Converter\Argument\TypeConverterInterface;

/**
 * Class StringType
 */
class StringType extends AbstractType implements TypeConverterInterface
{
    const TYPE_STRING = 'string';

    /**
     * @param array $node
     * @return string
     */
    public function convert(array $node)
    {
        return (string) $node[self::VALUE];
    }
}
